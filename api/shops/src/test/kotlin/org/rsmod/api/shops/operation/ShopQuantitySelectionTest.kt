package org.rsmod.api.shops.operation

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.ItemServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.market.MarketPrices
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.shops.currency.InvObjShopCurrency
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.api.shops.selectedOperation
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.shop.Shop
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ShopQuantitySelectionTest {
    @Test
    fun `left click buy after selecting quantity buys exactly that many`() {
        val single = Fixture()
        single.player.inv[0] = InvObj("obj.coins", 1000)
        single.leftClickShop(quantityMode = 1)
        val unitPrice = 1000 - single.player.inv.count("obj.coins")

        val f = Fixture()
        f.player.inv[0] = InvObj("obj.coins", 1000)
        f.leftClickShop(quantityMode = 2)

        assertEquals(5, f.player.inv.count("obj.bronze_axe"))
        assertEquals(5, f.shop.inv.count("obj.bronze_axe"))
        assertEquals(1000 - 5 * unitPrice, f.player.inv.count("obj.coins"))
    }

    @Test
    fun `left click sell after selecting quantity sells exactly that many`() {
        val f = Fixture()
        for (slot in 0 until 8) f.player.inv[slot] = InvObj("obj.bronze_axe")
        f.leftClickSide(quantityMode = 2)

        assertEquals(3, f.player.inv.count("obj.bronze_axe"))
        assertEquals(15, f.shop.inv.count("obj.bronze_axe"))
        assertTrue(f.player.inv.count("obj.coins") > 0)
    }

    @Test
    fun `left click with the 50 button selected sells up to 50`() {
        val f = Fixture()
        for (slot in 0 until 20) f.player.inv[slot] = InvObj("obj.bronze_axe")
        f.leftClickSide(quantityMode = 4)

        assertEquals(0, f.player.inv.count("obj.bronze_axe"))
        assertEquals(30, f.shop.inv.count("obj.bronze_axe"))
    }

    @Test
    fun `value mode and op6 only report value and move no items`() {
        for ((mode, op) in listOf(0 to IfButtonOp.Op1, 3 to IfButtonOp.Op6)) {
            val f = Fixture()
            f.player.inv[0] = InvObj("obj.coins", 1000)
            f.player.inv[1] = InvObj("obj.bronze_axe")
            val playerBefore = f.player.inv.toList()
            val shopBefore = f.shop.inv.toList()

            f.selectQuantity(mode)
            val resolved = checkNotNull(f.player.selectedOperation(op))
            f.operations.shopInvOp(f.player, f.player.inv, f.shop, f.slot, resolved)
            f.operations.sideInvOp(f.player, f.player.inv, f.shop, 1, resolved)

            assertEquals(playerBefore, f.player.inv.toList())
            assertEquals(shopBefore, f.shop.inv.toList())
        }
    }

    @Test
    fun `explicit right click ops ignore the selected quantity`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.coins", 1000)
        f.selectQuantity(4)
        f.operations.shopInvOp(
            f.player,
            f.player.inv,
            f.shop,
            f.slot,
            checkNotNull(f.player.selectedOperation(IfButtonOp.Op2)),
        )
        assertEquals(1, f.player.inv.count("obj.bronze_axe"))
    }

    private class Fixture {
        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                client = SilentClient()
                uuid = 894L
                slotId = 1
                assignUid()
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
            }
        val shop =
            Shop(
                inv = Inventory.create("inv.generalshop1"),
                currency = "currency.standard_gp",
                buyPercentage = 40.0,
                sellPercentage = 130.0,
                changePercentage = 0.0,
            )
        val slot = 0
        val operations =
            StandardCurrencyShopOperations(
                InvObjShopCurrency("obj.coins", "coin", "coins"),
                ShopRestockProcess(MapClock()),
                object : MarketPrices {
                    override fun get(type: ItemServerType): Int? = null
                },
            )

        init {
            shop.inv[slot] = InvObj("obj.bronze_axe", 10)
        }

        fun selectQuantity(mode: Int) {
            VarPlayerIntMapSetter.set(player, "varbit.shop_quantity", mode)
        }

        fun leftClickShop(quantityMode: Int) {
            selectQuantity(quantityMode)
            val op = checkNotNull(player.selectedOperation(IfButtonOp.Op1))
            operations.shopInvOp(player, player.inv, shop, slot, op)
        }

        fun leftClickSide(quantityMode: Int) {
            selectQuantity(quantityMode)
            val op = checkNotNull(player.selectedOperation(IfButtonOp.Op1))
            operations.sideInvOp(player, player.inv, shop, 0, op)
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
        private val RESTORED = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            val root =
                generateSequence(Path.of("").toAbsolutePath()) { it.parent }
                    .first { Files.isRegularFile(it.resolve("game.yml")) }
            val revision =
                Files.readAllLines(root.resolve("game.yml"))
                    .first { it.startsWith("revision:") }
                    .substringAfter(':')
                    .trim()
                    .substringBefore('.')
                    .toInt()
            ServerCacheManager.init(revision).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            RESTORED += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            RESTORED.asReversed().forEach { it() }
            RESTORED.clear()
        }
    }
}
