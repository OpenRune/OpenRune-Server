@file:OptIn(UncheckedType::class)

package org.rsmod.api.shops.operation

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InvStackType
import dev.openrune.types.InventoryServerType
import dev.openrune.types.util.UncheckedType
import io.mockk.mockk
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
import org.rsmod.api.player.events.PlayerShopEvents
import org.rsmod.api.shops.currency.InvObjShopCurrency
import org.rsmod.api.shops.restock.ShopRestockProcess
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.shop.Shop
import org.rsmod.game.type.cert
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class ShopPrepareEventsTest {
    @Test
    fun `no subscriber charges the normal price and depletes stock`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(COINS_ID, 1000)
        val coinsBefore = f.coins()

        f.buy(request = 2)

        assertEquals(2, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK - 2, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(coinsBefore - f.bought.single().cost, f.coins())
    }

    @Test
    fun `free buys with no coins and takes no coins`() {
        val f = Fixture { free = true }

        f.buy(request = 3)

        assertEquals(3, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK - 3, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(0, f.coins())
        assertEquals(0, f.bought.single().cost)
    }

    @Test
    fun `free leaves coins already held untouched`() {
        val f = Fixture { free = true }
        f.player.inv[0] = InvObj(COINS_ID, 500)

        f.buy(request = 1)

        assertEquals(500, f.coins())
        assertEquals(1, f.count(f.player.inv, DAGGER_ID))
    }

    @Test
    fun `noted delivery takes one slot and the same stock`() {
        val f = Fixture {
            free = true
            item = cert(DAGGER)
        }
        f.fillInv(leaveFree = 1)

        f.buy(request = 5)

        assertEquals(5, f.count(f.player.inv, cert(DAGGER).id))
        assertEquals(0, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK - 5, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(DAGGER_ID, f.bought.single().item.id)
    }

    @Test
    fun `free unnoted delivery is limited to free space`() {
        val f = Fixture { free = true }
        f.fillInv(leaveFree = 2)

        f.buy(request = 5)

        assertEquals(2, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK - 2, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(2, f.bought.single().count)
    }

    @Test
    fun `a full inventory changes nothing and publishes no purchase`() {
        val f = Fixture { free = true }
        f.fillInv(leaveFree = 0)

        f.buy(request = 1)

        assertEquals(0, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(emptyList<PlayerShopEvents.Bought>(), f.bought)
    }

    @Test
    fun `noted delivery that would overflow its stack changes nothing`() {
        val f = Fixture {
            free = true
            item = cert(DAGGER)
        }
        f.player.inv[0] = InvObj(cert(DAGGER).id, Int.MAX_VALUE)

        f.buy(request = 5)

        assertEquals(Int.MAX_VALUE, f.count(f.player.inv, cert(DAGGER).id))
        assertEquals(STOCK, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(emptyList<PlayerShopEvents.Bought>(), f.bought)
    }

    @Test
    fun `undepleted stock stays while the player still pays`() {
        val f = Fixture { depletesStock = false }
        f.player.inv[0] = InvObj(COINS_ID, 1000)

        f.buy(request = 2)

        assertEquals(2, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(1000 - f.bought.single().cost, f.coins())
    }

    @Test
    fun `undepleted free stock with no space reports no space and moves nothing`() {
        val f = Fixture {
            free = true
            depletesStock = false
        }
        f.fillInv(leaveFree = 0)

        f.buy(request = 1)

        assertEquals(0, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(emptyList<PlayerShopEvents.Bought>(), f.bought)
    }

    @Test
    fun `an unaffordable purchase moves nothing`() {
        val f = Fixture()

        f.buy(request = 1)

        assertEquals(0, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(emptyList<PlayerShopEvents.Bought>(), f.bought)
    }

    @Test
    fun `a refused purchase moves nothing`() {
        val f = Fixture { refusal = "No." }
        f.player.inv[0] = InvObj(COINS_ID, 1000)

        f.buy(request = 1)

        assertEquals(1000, f.coins())
        assertEquals(STOCK, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(emptyList<PlayerShopEvents.Bought>(), f.bought)
    }

    @Test
    fun `a refused sale keeps the item and pays nothing`() {
        val f = Fixture(onSell = { refusal = "The shop won't buy that." })
        f.player.inv[0] = InvObj(DAGGER_ID, 1)

        f.ops.invSell(f.player, f.player.inv, f.shop, 0, request = 1)

        assertEquals(1, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(0, f.coins())
        assertEquals(emptyList<PlayerShopEvents.Sold>(), f.sold)
    }

    @Test
    fun `a sale reports the unnoted item when notes are sold`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(cert(DAGGER).id, 3)

        f.ops.invSell(f.player, f.player.inv, f.shop, 0, request = 3)

        assertEquals(0, f.count(f.player.inv, cert(DAGGER).id))
        assertEquals(STOCK + 3, f.count(f.shop.inv, DAGGER_ID))
        val sold = f.sold.single()
        assertEquals(DAGGER_ID, sold.item.id)
        assertEquals(3, sold.count)
        assertEquals(sold.payment, f.coins())
    }

    @Test
    fun `a zero base cost sale takes the item for no coins`() {
        val f = Fixture(onSell = { baseCost = 0 })
        f.player.inv[0] = InvObj(DAGGER_ID, 1)

        f.ops.invSell(f.player, f.player.inv, f.shop, 0, request = 1)

        assertEquals(0, f.count(f.player.inv, DAGGER_ID))
        assertEquals(STOCK + 1, f.count(f.shop.inv, DAGGER_ID))
        assertEquals(0, f.coins())
    }

    private class Fixture(
        onSell: PlayerShopEvents.SellPrepare.() -> Unit = {},
        onBuy: PlayerShopEvents.BuyPrepare.() -> Unit = {},
    ) {
        val bought = mutableListOf<PlayerShopEvents.Bought>()
        val sold = mutableListOf<PlayerShopEvents.Sold>()
        val player = Player()
        val shop =
            Shop(
                inv =
                    Inventory(
                        InventoryServerType(size = 10, stack = InvStackType.Always),
                        arrayOfNulls(10),
                    ).also { it[0] = InvObj(DAGGER_ID, STOCK) },
                currency = "currency.standard_gp",
                buyPercentage = 60.0,
                sellPercentage = 100.0,
                changePercentage = 2.0,
            )
        val ops: StandardCurrencyShopOperations

        init {
            player.inv = Inventory(InventoryServerType(size = 28), arrayOfNulls(28))
            val bus = EventBus()
            bus.subscribeUnbound(PlayerShopEvents.BuyPrepare::class.java) { onBuy() }
            bus.subscribeUnbound(PlayerShopEvents.SellPrepare::class.java) { onSell() }
            bus.subscribeUnbound(PlayerShopEvents.Bought::class.java) { bought += this }
            bus.subscribeUnbound(PlayerShopEvents.Sold::class.java) { sold += this }
            ops =
                StandardCurrencyShopOperations(
                    currency = InvObjShopCurrency("obj.coins", "coin"),
                    restockProcess = ShopRestockProcess(MapClock()),
                    marketPrices = mockk(relaxed = true),
                    eventBus = bus,
                )
        }

        fun buy(request: Int) = ops.shopBuy(player, player.inv, shop, 0, request)

        fun coins() = count(player.inv, COINS_ID)

        fun count(inv: Inventory, id: Int) =
            inv.filterNotNull().filter { it.id == id }.sumOf { it.count }

        fun fillInv(leaveFree: Int) {
            for (slot in 0 until player.inv.size - leaveFree) {
                player.inv[slot] = InvObj(FILLER_ID, 1)
            }
        }
    }

    private companion object {
        const val DAGGER = "obj.bronze_dagger"
        const val STOCK = 10
        val DAGGER_ID by lazy { DAGGER.asRSCM(RSCMType.OBJ) }
        val COINS_ID by lazy { "obj.coins".asRSCM(RSCMType.OBJ) }
        val FILLER_ID by lazy { "obj.bronze_sword".asRSCM(RSCMType.OBJ) }
        val RESTORED = mutableListOf<() -> Unit>()

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
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
                val old = field.get(null)
                RESTORED += { field.set(null, old) }
            }
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
