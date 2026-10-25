package org.rsmod.content.interfaces.omnishop

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
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
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.shops.currency.VarBitShopCurrency
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class OmnishopPurchaseTest {
    @Test
    fun `native Tithe stock debits points per transaction and repeated purchases cannot overspend`() {
        val f = Fixture(16)
        f.buy(6, 2)
        assertEquals(5, f.count("obj.grape_seed"))
        assertEquals(6, f.points)
        f.buy(6, 2)
        assertEquals(8, f.count("obj.grape_seed"))
        assertEquals(0, f.points)
        f.buy(6, 1)
        assertEquals(8, f.count("obj.grape_seed"))
        assertEquals(0, f.points)
    }

    @Test
    fun `full inventory preserves points and every item while partial space buys only what fits`() {
        val f = Fixture(300)
        f.fill(28)
        val before = f.player.inv.objs.toList()
        f.buy(8, 2)
        assertEquals(before, f.player.inv.objs.toList())
        assertEquals(300, f.points)
        f.player.inv[26] = null
        f.player.inv[27] = null
        f.buy(8, 2)
        assertEquals(2, f.count("obj.nzone_herbbox"))
        assertEquals(26, f.count("obj.spade"))
        assertEquals(240, f.points)
    }

    @Test
    fun `native noted stock merges into a full inventory without replacing other items`() {
        val f = Fixture(20)
        val compost = f.type("obj.bucket_compost")
        f.player.inv[0] = InvObj(checkNotNull(ServerCacheManager.getItem(compost.certlink)), 4)
        f.fill(28)
        f.buy(4, 3)
        assertEquals(14, f.player.inv[0]?.count)
        assertEquals(compost.certlink, f.player.inv[0]?.id)
        assertEquals(27, f.count("obj.spade"))
        assertEquals(10, f.points)
    }

    @Test
    fun `wrong shop closed shop invalid index and unavailable option cannot spend points`() {
        val f = Fixture(300)
        f.buy(-1, 1)
        f.buy(16, 1)
        f.buy(6, 0)
        f.buy(6, 5)
        f.buy(6, Int.MAX_VALUE)
        f.buy(6, 1, shop = "dbrow.forestry_shop".asRSCM(RSCMType.DBROW))
        f.player.omnishopLastShop = -1
        f.buy(6, 1)
        assertEquals(300, f.points)
        assertEquals(List<InvObj?>(28) { null }, f.player.inv.objs.toList())
    }

    @Test
    fun `reward refusal leaves points and inventory untouched and never completes`() {
        val f = Fixture(300)
        f.events.subscribeUnbound(OmnishopPurchaseCheck::class.java) { failure = "Not unlocked." }
        var completed = 0
        f.events.subscribeUnbound(OmnishopPurchaseCompleted::class.java) { completed++ }
        f.buy(7, 2)
        assertEquals(300, f.points)
        assertEquals(0, f.count("obj.grape_blessing"))
        assertEquals(0, completed)
    }

    @Test
    fun `zero multiplier unlock commits without a dummy item and can be rejected on repeat`() {
        val f = Fixture(50)
        f.fill(28)
        var unlocked = false
        f.events.subscribeUnbound(OmnishopPurchaseCheck::class.java) {
            maximum = 1
            if (unlocked) failure = "Already unlocked."
        }
        f.events.subscribeUnbound(OmnishopPurchaseCompleted::class.java) { unlocked = true }
        f.buy(14, 4)
        assertEquals(true, unlocked)
        assertEquals(0, f.points)
        assertEquals(28, f.count("obj.spade"))
        f.buy(14, 1)
        assertEquals(0, f.points)
        assertEquals(28, f.count("obj.spade"))
    }

    @Test
    fun `inventory currency payment frees a slot and mixed points are charged only on success`() {
        val f = Fixture(10)
        val coins = f.type("obj.coins")
        val payments = listOf(OmnishopPayment(listOf(coins), null, 25), f.payment(10))
        f.player.inv[0] = InvObj(coins, 25)
        f.fill(28)
        assertEquals(1, f.purchase(payments, f.type("obj.spade")))
        assertEquals(0, f.count("obj.coins"))
        assertEquals(28, f.count("obj.spade"))
        assertEquals(0, f.points)
        assertEquals(0, f.purchase(payments, f.type("obj.spade")))
        assertEquals(28, f.count("obj.spade"))
        assertEquals(0, f.points)
    }

    @Test
    fun `failed mixed payment rolls back both items and points after tentative deletes`() {
        val f = Fixture(10)
        val coins = f.type("obj.coins")
        f.player.inv[0] = InvObj(coins, 100)
        f.fill(28)
        val before = f.player.inv.objs.toList()
        assertEquals(0, f.purchase(listOf(OmnishopPayment(listOf(coins), null, 25), f.payment(10)),
            f.type("obj.grape_seed")))
        assertEquals(before, f.player.inv.objs.toList())
        assertEquals(10, f.points)
        f.player.inv[0] = null
        assertEquals(0, f.purchase(listOf(OmnishopPayment(listOf(coins), null, 25), f.payment(10)),
            f.type("obj.grape_seed")))
        assertEquals(0, f.count("obj.grape_seed"))
        assertEquals(10, f.points)
    }

    @Test
    fun `stack overflow and oversized multiplier cannot debit points or wrap output count`() {
        val f = Fixture(10)
        val coins = f.type("obj.coins")
        f.player.inv[0] = InvObj(coins, Int.MAX_VALUE - 2)
        assertEquals(0, f.purchase(listOf(f.payment(1)), coins, multiplier = 3))
        assertEquals(Int.MAX_VALUE - 2, f.count("obj.coins"))
        assertEquals(10, f.points)
        assertEquals(0, f.purchase(listOf(f.payment(Int.MAX_VALUE.toLong() + 1)), coins))
        assertEquals(10, f.points)
        f.player.inv[0] = InvObj(coins, Int.MAX_VALUE - 3)
        assertEquals(1, f.purchase(listOf(f.payment(1)), coins, multiplier = 3, requested = 50))
        assertEquals(Int.MAX_VALUE, f.count("obj.coins"))
        assertEquals(9, f.points)
    }

    @Test
    fun `native blessing multiplier grants twenty items per paid transaction`() {
        val f = Fixture(5)
        f.buy(7, 2)
        assertEquals(100, f.count("obj.grape_blessing"))
        assertEquals(0, f.points)
    }

    @Test
    fun `reward item vars are committed only with the successful payment`() {
        val f = Fixture(30)
        f.events.subscribeUnbound(OmnishopPurchaseCheck::class.java) { outputVars = 1 }
        f.buy(9, 1)
        assertEquals(0, f.points)
        assertEquals("obj.seedbox".asRSCM(RSCMType.OBJ), f.player.inv[0]?.id)
        assertEquals(1, f.player.inv[0]?.vars)
    }

    @Test
    fun `native Forestry purchase combines noted and unnoted object currencies atomically`() {
        val f = Fixture(0)
        val shop = "dbrow.forestry_shop".asRSCM(RSCMType.DBROW)
        f.player.omnishopLastShop = shop
        f.player.inv[0] = InvObj("obj.forestry_currency", 20)
        f.player.inv[1] = InvObj("obj.cert_oak_logs", 8)
        f.player.inv[2] = InvObj("obj.oak_logs")
        f.player.inv[3] = InvObj("obj.oak_logs")
        f.player.inv[4] = InvObj("obj.cert_willow_logs", 5)
        f.fill(28)
        f.buy(1, 1, shop)
        assertEquals(1, f.count("obj.forestry_secateurs_blade"))
        assertEquals(0, f.count("obj.forestry_currency"))
        assertEquals(0, f.count("obj.cert_oak_logs"))
        assertEquals(0, f.count("obj.oak_logs"))
        assertEquals(0, f.count("obj.cert_willow_logs"))
        assertEquals(23, f.count("obj.spade"))
    }

    @Test
    fun `object currency sell rolls back without output space and can use its freed source slot`() {
        val f = Fixture(0)
        f.player.omnishopLastShop = "dbrow.forestry_shop".asRSCM(RSCMType.DBROW)
        f.player.inv[0] = InvObj("obj.forestry_pheasant_feathers", 2)
        f.fill(28)
        f.sell("obj.forestry_pheasant_feathers", 1)
        assertEquals(2, f.count("obj.forestry_pheasant_feathers"))
        assertEquals(0, f.count("obj.forestry_currency"))
        f.player.inv[0] = InvObj("obj.forestry_pheasant_feathers")
        f.sell("obj.forestry_pheasant_feathers", 1)
        assertEquals(0, f.count("obj.forestry_pheasant_feathers"))
        assertEquals(5, f.count("obj.forestry_currency"))
        assertEquals(27, f.count("obj.spade"))
    }

    private class Fixture(points: Int) {
        val events = EventBus()
        private val coroutine = GameCoroutine("omnishop-test")
        private val script = OmnishopScript()
        private val shop = "dbrow.tithe_shop_data".asRSCM(RSCMType.DBROW)

        @OptIn(InternalApi::class)
        val player = Player().apply {
            client = SilentClient()
            uuid = 798L
            slotId = 1
            assignUid()
            inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
            activeCoroutine = coroutine
            omnishopLastShop = shop
        }
        private val access = ProtectedAccess(player, coroutine,
            ProtectedAccessContextFactory.empty().copy(getEventBus = { events }))

        var points: Int
            get() = player.vars[POINTS]
            set(value) { VarPlayerIntMapSetter.set(player, POINTS, value) }

        init { this.points = points }

        fun buy(index: Int, option: Int, shop: Int = this.shop) {
            with(script) { access.buy(shop, index, option) }
        }
        fun sell(obj: String, quantity: Int) {
            with(script) { access.sell(type(obj), quantity) }
        }
        fun type(obj: String) = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
        fun count(obj: String) = player.inv.physicalCount(obj)
        fun fill(count: Int) {
            for (slot in 0 until count) if (player.inv[slot] == null) player.inv[slot] = InvObj("obj.spade")
        }
        fun payment(price: Long) = OmnishopPayment(emptyList(), VarBitShopCurrency(POINTS, "Tithe point"), price)
        fun purchase(payments: List<OmnishopPayment>, output: ItemServerType?, multiplier: Int = 1,
            requested: Int = 1) = buyOmnishopStock(player, payments, output, multiplier, requested)
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
        private const val POINTS = "varbit.hosidius_tithe_rewardpoints"
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
