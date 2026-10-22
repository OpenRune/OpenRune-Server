package org.rsmod.content.areas.city.draynor

import dev.openrune.ServerCacheManager
import dev.openrune.types.ItemServerType
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.currency.InvObjShopCurrency
import org.rsmod.api.shops.operation.StandardCurrencyShopOperations
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.content.areas.city.draynor.npcs.DraynorVillageNpcs
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class NedShopTest {
    private lateinit var previousPolicy: QuestRequirementPolicy

    @BeforeEach
    fun respectQuestProgress() {
        previousPolicy = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
    }

    @AfterEach
    fun restoreQuestPolicy() {
        QuestRequirements.install(previousPolicy)
    }

    @Test
    fun `buying rope transfers stock and charges the normal price`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.buy()
        assertEquals(82, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(ROPE))
        assertEquals(29, f.shop.inv.count(ROPE))
    }

    @Test
    fun `Fremennik Isles discount applies to purchases without discounting buyback`() {
        QuestRequirements.install(
            QuestRequirementPolicy(
                QuestRequirementMode.VirtualCompletions,
                virtualCompletions = setOf("quest_fremennikisles"),
            )
        )
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.buy()
        assertEquals(84, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(ROPE))
        assertEquals(29, f.shop.inv.count(ROPE))
        f.sell()
        assertEquals(87, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ROPE))
        assertEquals(30, f.shop.inv.count(ROPE))
    }

    @Test
    fun `full inventory does not consume coins or stock`() {
        val f = Fixture()
        for (slot in 0 until 28) f.player.inv[slot] = InvObj("obj.bronze_dagger", 1)
        f.player.inv[0] = InvObj(COINS, 100)
        f.buy()
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ROPE))
        assertEquals(30, f.shop.inv.count(ROPE))
        assertEquals(27, f.player.inv.count("obj.bronze_dagger"))
    }

    @Test
    fun `repeated purchase after spending the last coins cannot duplicate rope`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 18)
        f.buy()
        f.buy()
        assertEquals(0, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(ROPE))
        assertEquals(29, f.shop.inv.count(ROPE))
    }

    @Test
    fun `stock depleted before purchase costs nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100)
        f.shop.inv[0] = InvObj(ROPE, 0)
        f.buy()
        assertEquals(100, f.player.inv.count(COINS))
        assertEquals(0, f.player.inv.count(ROPE))
        assertEquals(0, f.shop.inv.count(ROPE))
    }

    @Test
    fun `selling with a maximum coin stack preserves rope and shop stock`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, Int.MAX_VALUE)
        f.player.inv[1] = InvObj(ROPE, 1)
        f.sell()
        assertEquals(Int.MAX_VALUE, f.player.inv.count(COINS))
        assertEquals(1, f.player.inv.count(ROPE))
        assertEquals(30, f.shop.inv.count(ROPE))
    }

    private class Fixture {
        private val events = EventBus()
        private val coroutine = GameCoroutine("ned-shop-test")
        private val context = ProtectedAccessContextFactory.empty().copy(
            getEventBus = { events },
            getAlignment = { TextAlignment() },
        )
        private val operations = StandardCurrencyShopOperations(
            InvObjShopCurrency(COINS, "coin"),
            ShopRestockProcess(MapClock(100)),
            object : MarketPrices {
                override fun get(type: ItemServerType): Int? = null
            },
        )

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = object : Client<Any, Any> {
                override fun write(message: Any) {}
                override fun close() {}
                override fun read(player: Player) {}
                override fun flush() {}
                override fun flushHighPriority() {}
                override fun unregister(service: Any, player: Player) {}
            }
            coords = CoordGrid(3098, 3257, 0)
            currentMapClock = 100
            processedMapClock = 100
            inv = Inventory.create("inv.inv")
            invMap["inv.inv"] = inv
        }

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(DraynorVillageNpcs(Shops(events))) { scripts.startup() }
            val body: suspend () -> Unit = {
                val access = ProtectedAccess(player, coroutine, context)
                assertTrue(events.publish(access, NpcEvents.Op3(Npc("npc.ned", player.coords.translateZ(1)))))
            }
            body.startCoroutine(object : Continuation<Unit> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Unit>) { result.getOrThrow() }
            })
        }

        val shop = checkNotNull(player.openedShop) { "Ned's Trade option did not open the rope shop" }

        fun buy() = operations.shopBuy(player, player.inv, shop, 0, 1)

        fun sell() = operations.invSell(player, player.inv, shop, 1, 1)
    }

    companion object {
        private const val COINS = "obj.coins"
        private const val ROPE = "obj.rope"
        private val RESTORE = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun loadCache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORE += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORE += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restoreTransactions() {
            RESTORE.asReversed().forEach { it() }
            RESTORE.clear()
        }
    }
}
