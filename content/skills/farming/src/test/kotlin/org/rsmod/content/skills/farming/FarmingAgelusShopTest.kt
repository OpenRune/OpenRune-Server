package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.ifOpenTop
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.cost.StandardGpCostCalculations as Cost
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.events.SuspendEvent
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
class FarmingAgelusShopTest {
    @Test
    fun `native quantity buttons buy exactly the quoted gp and stock delta`() {
        for (quantity in listOf(1, 5, 10)) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 100_000)
            f.player.inv[1] = InvObj(LOGS, 7)
            f.select(quantity)
            val cost = f.buyQuote(RAKE, 10, quantity)
            f.buy(RAKE)
            assertEquals(100_000 - cost, f.count(COINS), "quantity=$quantity")
            assertEquals(quantity, f.count(RAKE), "quantity=$quantity")
            assertEquals(10 - quantity, f.stock(RAKE), "quantity=$quantity")
            assertEquals(7, f.count(LOGS))
            assertEquals(f.xp, f.player.statMap.getFineXP("stat.farming"))
        }
    }

    @Test
    fun `value mode and repeated clicks never spend beyond the committed quantity`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 100_000)
        f.select(0)
        val before = f.snapshot()
        f.buy(RAKE)
        assertEquals(before, f.snapshot())
        f.select(1)
        val cost = f.buyQuote(RAKE, 10, 3)
        repeat(3) { f.buy(RAKE) }
        assertEquals(100_000 - cost, f.count(COINS))
        assertEquals(3, f.count(RAKE))
        assertEquals(7, f.stock(RAKE))
    }

    @Test
    fun `oversized request is capped by stock and the sold out slot refuses further buys`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_000_000)
        f.select(50)
        val cost = f.buyQuote(SPADE, 10, 10)
        f.buy(SPADE)
        assertEquals(1_000_000 - cost, f.count(COINS))
        assertEquals(10, f.count(SPADE))
        assertEquals(0, f.stock(SPADE))
        val exhausted = f.snapshot()
        f.buy(SPADE)
        assertEquals(exhausted, f.snapshot())
    }

    @Test
    fun `permanently empty crop slots refuse every quantity without side effects`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_000_000)
        val before = f.snapshot()
        for (quantity in listOf(0, 1, 5, 10, 50)) {
            f.select(quantity)
            f.buy(GRAIN)
            assertEquals(before, f.snapshot(), "quantity=$quantity")
        }
    }

    @Test
    fun `insufficient gp buys only the affordable prefix or nothing`() {
        val f = Fixture()
        val first = f.buyQuote(RAKE, 10, 1)
        f.player.inv[0] = InvObj(COINS, first - 1)
        f.select(5)
        val broke = f.snapshot()
        f.buy(RAKE)
        assertEquals(broke, f.snapshot())

        val two = f.buyQuote(RAKE, 10, 2)
        val three = f.buyQuote(RAKE, 10, 3)
        f.player.inv[0] = InvObj(COINS, three - 1)
        f.buy(RAKE)
        assertEquals(three - 1 - two, f.count(COINS))
        assertEquals(2, f.count(RAKE))
        assertEquals(8, f.stock(RAKE))
    }

    @Test
    fun `full inventory refuses unless spending the whole gp stack frees its slot`() {
        val f = Fixture()
        f.select(1)
        val price = f.buyQuote(RAKE, 10, 1)
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj(LOGS)
        f.player.inv[0] = InvObj(COINS, price + 5)
        val full = f.snapshot()
        f.buy(RAKE)
        assertEquals(full, f.snapshot())

        f.player.inv[0] = InvObj(COINS, price)
        f.buy(RAKE)
        assertEquals(0, f.count(COINS))
        assertEquals(1, f.count(RAKE))
        assertEquals(27, f.count(LOGS))
        assertEquals(9, f.stock(RAKE))
    }

    @Test
    fun `mismatched client item and non shop items are refused`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 10_000)
        f.player.inv[1] = InvObj(LOGS, 5)
        f.player.inv[2] = InvObj(SPADE)
        f.select(1)
        val before = f.snapshot()
        f.button("component.shopmain:items", f.slot(RAKE) + 1, DIBBER, IfButtonOp.Op1)
        f.button("component.shopside:items", 1, SPADE, IfButtonOp.Op1)
        f.button("component.shopside:items", 1, LOGS, IfButtonOp.Op1)
        f.button("component.shopside:items", 0, COINS, IfButtonOp.Op1)
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `selling pays the quoted gp and raises stock by the committed quantity`() {
        for (quantity in listOf(1, 5, 10)) {
            val f = Fixture()
            f.player.inv[0] = InvObj(COINS, 50)
            f.player.inv[1] = InvObj(SPADE, 1)
            f.player.inv[2] = InvObj(SPADE, 1)
            f.player.inv[3] = InvObj(SPADE, 1)
            f.player.inv[4] = InvObj(SPADE, 1)
            f.player.inv[5] = InvObj(SPADE, 1)
            f.player.inv[6] = InvObj(LOGS, 3)
            f.select(quantity)
            f.sell(1, SPADE)
            // Non-stackable sales draw from every spade slot, capped by the selected quantity.
            val sold = minOf(quantity, 5)
            val payment = f.sellQuote(SPADE, 10, sold)
            assertEquals(50 + payment, f.count(COINS), "quantity=$quantity")
            assertEquals(5 - sold, f.count(SPADE), "quantity=$quantity")
            assertEquals(10 + sold, f.stock(SPADE), "quantity=$quantity")
            assertEquals(3, f.count(LOGS))
            assertEquals(f.xp, f.player.statMap.getFineXP("stat.farming"))
        }
    }

    @Test
    fun `repeated sales and a buy back conserve gp stock and items exactly`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, 1_000)
        for (slot in 1..3) f.player.inv[slot] = InvObj(SPADE)
        f.select(1)
        var expectedCoins = 1_000
        for (i in 0 until 3) {
            expectedCoins += f.sellQuote(SPADE, 10 + i, 1)
            f.sell(1 + i, SPADE)
            assertEquals(expectedCoins, f.count(COINS))
            assertEquals(2 - i, f.count(SPADE))
            assertEquals(11 + i, f.stock(SPADE))
        }
        f.select(5)
        f.buy(SPADE)
        val five = f.buyQuote(SPADE, 13, 5)
        assertEquals(expectedCoins - five, f.count(COINS))
        assertEquals(5, f.count(SPADE))
        assertEquals(8, f.stock(SPADE))
    }

    @Test
    fun `gp overflow sale is rolled back leaving item and stock untouched`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS, Int.MAX_VALUE)
        f.player.inv[1] = InvObj(SPADE)
        f.select(1)
        val before = f.snapshot()
        f.sell(1, SPADE)
        assertEquals(before, f.snapshot())
    }

    private class Fixture {
        private val events = EventBus()
        private val operations = ShopOperationMap()
        private val restock = ShopRestockProcess(MapClock())
        private val prices = object : MarketPrices {
            override fun get(type: ItemServerType): Int? = null
        }

        val client = SilentClient()

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = this@Fixture.client
            uuid = 896L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            ifOpenTop("interface.toplevel_osrs_stretch")
        }
        val xp: Int = player.statMap.getFineXP("stat.farming")

        init {
            val context = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(ShopScript(operations, restock, prices)) { context.startup() }
            with(FarmingSeedShopsScript(Shops(events))) { context.startup() }
            publish(NpcEvents.Op3(Npc("npc.fortis_shop_farming", CoordGrid(1700, 3140, 0))))
            check(player.openedShop != null)
        }

        fun slot(obj: String): Int {
            val index = checkNotNull(player.openedShop).inv.type.stock.indexOfFirst {
                it.obj == obj.asRSCM()
            }
            check(index >= 0) { obj }
            return index
        }

        fun cost(obj: String): Int = checkNotNull(ServerCacheManager.getItem(obj.asRSCM())).cost

        fun buyQuote(obj: String, stock: Int, quantity: Int): Int =
            (0 until quantity).sumOf {
                Cost.calculateShopSellSingleValue(10, stock - it, cost(obj), 100.0, 1.0)
            }

        fun sellQuote(obj: String, stock: Int, quantity: Int): Int =
            (0 until quantity).sumOf {
                Cost.calculateShopBuySingleValue(10, stock + it, cost(obj), 70.0, 1.0)
            }

        fun buy(obj: String) =
            button("component.shopmain:items", slot(obj) + 1, obj, IfButtonOp.Op1)

        fun sell(invSlot: Int, obj: String) =
            button("component.shopside:items", invSlot, obj, IfButtonOp.Op1)

        fun select(quantity: Int) {
            val name = if (quantity == 0) "value" else "quantity$quantity"
            publish(IfModalButton(
                ServerCacheManager.fromComponent("component.shopmain:$name"), -1, null, IfButtonOp.Op1,
            ))
        }

        fun count(obj: String): Int = player.inv.physicalCount(obj)
        fun stock(obj: String): Int = checkNotNull(player.openedShop).inv.physicalCount(obj)
        fun snapshot() = player.inv.toList() to checkNotNull(player.openedShop).inv.toList()

        fun button(component: String, sub: Int, obj: String, op: IfButtonOp) {
            val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM()))
            publish(IfModalButton(ServerCacheManager.fromComponent(component), sub, type, op))
        }

        private fun publish(event: SuspendEvent<ProtectedAccess>) {
            val coroutine = GameCoroutine("agelus-shop-test")
            player.activeCoroutine = coroutine
            val access = ProtectedAccess(player, coroutine,
                ProtectedAccessContextFactory.empty().copy(getEventBus = { events }))
            var result: Result<Unit>? = null
            val action: suspend () -> Unit = { check(events.publish(access, event)) }
            try {
                action.startCoroutine(object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext
                    override fun resumeWith(outcome: Result<Unit>) { result = outcome }
                })
                checkNotNull(result).getOrThrow()
            } finally {
                player.activeCoroutine = null
            }
        }
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}
        override fun close() {}
        override fun read(player: Player) {}
        override fun flush() {}
        override fun flushHighPriority() {}
        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val COINS = "obj.coins"
        private const val LOGS = "obj.logs"
        private const val RAKE = "obj.rake"
        private const val DIBBER = "obj.dibber"
        private const val SPADE = "obj.spade"
        private const val GRAIN = "obj.grain"
        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic @BeforeAll fun cache() {
            val root = generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision = Files.readAllLines(root.resolve("game.yml"))
                .first { it.startsWith("revision:") }.substringAfter(':').trim().substringBefore('.').toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in listOf(
                "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
            )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic @AfterAll fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
