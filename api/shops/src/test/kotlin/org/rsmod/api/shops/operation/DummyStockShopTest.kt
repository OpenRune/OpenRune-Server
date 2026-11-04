package org.rsmod.api.shops.operation

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.InventoryServerType
import dev.openrune.types.ItemServerType
import java.nio.file.Files
import java.nio.file.Path
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
import org.rsmod.api.shops.currency.InvObjShopCurrency
import org.rsmod.api.shops.restock.ShopRestockProcess
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
class DummyStockShopTest {
    @Test
    fun `repeated catalogue purchases spend coins once each without consuming displayed stock`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.coins", 120)
        repeat(4) { f.buy() }
        assertEquals(3, f.player.inv.physicalCount("obj.plant_cure"))
        assertEquals(0, f.player.inv.physicalCount("obj.coins"))
        assertEquals(500, f.shop.inv.physicalCount("obj.plant_cure"))
    }

    @Test
    fun `failed full inventory receipt rolls back the coin debit and preserves stock`() {
        val f = Fixture()
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj("obj.spade")
        f.player.inv[0] = InvObj("obj.coins", 1000)
        val before = f.player.inv.toList()
        f.buy()
        assertEquals(before, f.player.inv.toList())
        assertEquals(500, f.shop.inv.physicalCount("obj.plant_cure"))
        f.player.inv[1] = null
        f.buy()
        assertEquals(960, f.player.inv.physicalCount("obj.coins"))
        assertEquals(1, f.player.inv.physicalCount("obj.plant_cure"))
        assertEquals(500, f.shop.inv.physicalCount("obj.plant_cure"))
    }

    @Test
    fun `catalogue cannot buy items back or mint coins from repeated sell operations`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - 1)
        f.player.inv[1] = InvObj("obj.plant_cure")
        val before = f.player.inv.toList()
        repeat(2) { f.operations.invSell(f.player, f.player.inv, f.shop, 1, 1) }
        assertEquals(before, f.player.inv.toList())
        assertEquals(500, f.shop.inv.physicalCount("obj.plant_cure"))
    }

    @Test
    fun `ordinary shop purchase still consumes real stock`() {
        val f = Fixture(dummy = false)
        f.player.inv[0] = InvObj("obj.coins", 40)
        f.buy()
        assertEquals(0, f.player.inv.physicalCount("obj.coins"))
        assertEquals(1, f.player.inv.physicalCount("obj.plant_cure"))
        assertEquals(499, f.shop.inv.physicalCount("obj.plant_cure"))
    }

    @Test
    fun `catalogue stack overflow leaves the seed count coins and stock unchanged`() {
        val f = Fixture()
        for (slot in f.player.inv.indices) f.player.inv[slot] = InvObj("obj.spade")
        f.player.inv[0] = InvObj("obj.potato_seed", Int.MAX_VALUE)
        f.player.inv[1] = InvObj("obj.coins", 1000)
        f.shop.inv[f.slot] = InvObj("obj.potato_seed", 500)
        val before = f.player.inv.toList()
        f.buy()
        assertEquals(before, f.player.inv.toList())
        assertEquals(500, f.shop.inv.physicalCount("obj.potato_seed"))
    }

    @Test
    fun `bulk seed decay starts strictly above two hundred excess and rounds to whole seeds`() {
        for ((excess, remaining) in listOf(200 to 199, 201 to 199, 299 to 297, 300 to 297)) {
            val inv = Inventory.create("inv.seed_stall")
            val stock = inv.getValue(0)
            inv[0] = stock.copy(count = stock.count + excess)
            val restock = ShopRestockProcess(MapClock(100))
            restock += inv
            restock.process()
            assertEquals(stock.count + remaining, inv.getValue(0).count)
        }
    }

    @Test
    fun `bulk normalization cannot overflow at maximum stock and deficits restore one seed`() {
        val inv = Inventory.create("inv.seed_stall")
        val stock = inv.getValue(0)
        val restock = ShopRestockProcess(MapClock(100))
        inv[0] = stock.copy(count = Int.MAX_VALUE)
        restock += inv
        restock.process()
        assertEquals(2_126_008_811, inv.getValue(0).count)
        inv[0] = stock.copy(count = stock.count - 1)
        restock.process()
        assertEquals(stock.count, inv.getValue(0).count)
    }

    private class Fixture(dummy: Boolean = true) {
        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 894L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        }
        private val catalog = Inventory.create("inv.farmer_supplies")
        val shop = Shop(
            inv = if (dummy) catalog else Inventory(
                catalog.type.copy(flags = catalog.type.flags and InventoryServerType.DUMMY_INV_FLAG.inv()),
                catalog.objs.copyOf(),
            ),
            currency = "currency.standard_gp",
            buyPercentage = 0.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
        )
        val slot = shop.inv.indexOfFirst { it?.id == "obj.plant_cure".asRSCM() }
        val operations = StandardCurrencyShopOperations(
            InvObjShopCurrency("obj.coins", "coin", "coins"),
            ShopRestockProcess(MapClock()),
            object : MarketPrices {
                override fun get(type: ItemServerType): Int? = null
            },
        )

        fun buy() = operations.shopBuy(player, player.inv, shop, slot, request = 1)
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
