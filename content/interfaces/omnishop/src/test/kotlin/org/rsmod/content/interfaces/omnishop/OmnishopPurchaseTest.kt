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
    fun `repeated purchases only spend what is owned across every currency`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.forestry_currency", 40)
        f.player.inv[1] = InvObj("obj.cert_oak_logs", 20)
        f.player.inv[2] = InvObj("obj.cert_willow_logs", 10)
        f.buy(BLADE, 4)
        assertEquals(2, f.count("obj.forestry_secateurs_blade"))
        assertEquals(0, f.count("obj.forestry_currency"))
        assertEquals(0, f.count("obj.cert_oak_logs"))
        assertEquals(0, f.count("obj.cert_willow_logs"))
        f.buy(BLADE, 4)
        assertEquals(2, f.count("obj.forestry_secateurs_blade"))
    }

    @Test
    fun `full inventory keeps every item and partial space buys only what fits`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.forestry_currency", 10000)
        f.fill(28)
        val before = f.player.inv.objs.toList()
        f.buy(CAPE_POUCH, 3)
        assertEquals(before, f.player.inv.objs.toList())
        f.player.inv[26] = null
        f.player.inv[27] = null
        f.buy(CAPE_POUCH, 3)
        assertEquals(2, f.count("obj.forestry_cape_pouch"))
        assertEquals(5000, f.count("obj.forestry_currency"))
    }

    @Test
    fun `object currency can be paid with noted and unnoted logs in one purchase`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.forestry_currency", 20)
        f.player.inv[1] = InvObj("obj.cert_oak_logs", 8)
        f.player.inv[2] = InvObj("obj.oak_logs")
        f.player.inv[3] = InvObj("obj.oak_logs")
        f.player.inv[4] = InvObj("obj.cert_willow_logs", 5)
        f.fill(28)
        f.buy(BLADE, 1)
        assertEquals(1, f.count("obj.forestry_secateurs_blade"))
        assertEquals(0, f.count("obj.forestry_currency"))
        assertEquals(0, f.count("obj.cert_oak_logs"))
        assertEquals(0, f.count("obj.oak_logs"))
        assertEquals(0, f.count("obj.cert_willow_logs"))
        assertEquals(23, f.count("obj.spade"))
    }

    @Test
    fun `noted output merges into a full inventory without replacing other items`() {
        val f = Fixture()
        val coins = f.type("obj.coins")
        val logs = f.type("obj.oak_logs")
        f.player.inv[0] = InvObj(checkNotNull(ServerCacheManager.getItem(logs.certlink)), 4)
        f.player.inv[1] = InvObj(coins, 30)
        f.fill(28)
        assertEquals(3, f.purchase(listOf(OmnishopPayment(listOf(coins), 10)), logs, requested = 3, cert = true))
        assertEquals(7, f.player.inv[0]?.count)
        assertEquals(logs.certlink, f.player.inv[0]?.id)
        assertEquals(0, f.count("obj.oak_logs"))
        assertEquals(26, f.count("obj.spade"))
    }

    @Test
    fun `wrong shop closed shop invalid index and unavailable option cannot spend anything`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.forestry_currency", 300)
        f.player.inv[1] = InvObj("obj.cert_oak_logs", 300)
        f.player.inv[2] = InvObj("obj.cert_willow_logs", 300)
        val before = f.player.inv.objs.toList()
        f.buy(-1, 1)
        f.buy(100, 1)
        f.buy(BLADE, 0)
        f.buy(BLADE, 5)
        f.buy(BLADE, Int.MAX_VALUE)
        f.buy(PHEASANT_FEATHERS, 1)
        f.buy(BLADE, 1, shop = -1)
        f.player.omnishopLastShop = -1
        f.buy(BLADE, 1)
        assertEquals(before, f.player.inv.objs.toList())
    }

    @Test
    fun `inventory currency payment frees a slot and a failed payment rolls back every deletion`() {
        val f = Fixture()
        val coins = f.type("obj.coins")
        val spade = f.type("obj.spade")
        val payments = listOf(OmnishopPayment(listOf(coins), 25))
        f.player.inv[0] = InvObj(coins, 100)
        f.fill(28)
        val before = f.player.inv.objs.toList()
        assertEquals(0, f.purchase(payments, spade))
        assertEquals(before, f.player.inv.objs.toList())
        f.player.inv[0] = InvObj(coins, 25)
        assertEquals(1, f.purchase(payments, spade))
        assertEquals(0, f.count("obj.coins"))
        assertEquals(28, f.count("obj.spade"))
    }

    @Test
    fun `unaffordable second payment leaves the first currency untouched`() {
        val f = Fixture()
        val coins = f.type("obj.coins")
        f.player.inv[0] = InvObj(coins, 100)
        val payments = listOf(
            OmnishopPayment(listOf(coins), 25),
            OmnishopPayment(listOf(f.type("obj.oak_logs")), 1),
        )
        assertEquals(0, f.purchase(payments, f.type("obj.spade")))
        assertEquals(100, f.count("obj.coins"))
        assertEquals(0, f.count("obj.spade"))
    }

    @Test
    fun `stack overflow and oversized prices cannot spend or wrap the output count`() {
        val f = Fixture()
        val coins = f.type("obj.coins")
        val logs = f.type("obj.oak_logs")
        f.player.inv[0] = InvObj(coins, Int.MAX_VALUE - 2)
        f.player.inv[1] = InvObj(logs, 5)
        assertEquals(0, f.purchase(listOf(OmnishopPayment(listOf(logs), 1)), coins, multiplier = 3))
        assertEquals(Int.MAX_VALUE - 2, f.count("obj.coins"))
        assertEquals(5, f.count("obj.oak_logs"))
        assertEquals(0, f.purchase(listOf(OmnishopPayment(listOf(logs), Int.MAX_VALUE.toLong() + 1)), coins))
        assertEquals(5, f.count("obj.oak_logs"))
        f.player.inv[0] = InvObj(coins, Int.MAX_VALUE - 3)
        assertEquals(1, f.purchase(listOf(OmnishopPayment(listOf(logs), 1)), coins, multiplier = 3, requested = 50))
        assertEquals(Int.MAX_VALUE, f.count("obj.coins"))
        assertEquals(4, f.count("obj.oak_logs"))
        assertEquals(0, f.purchase(listOf(OmnishopPayment(listOf(logs), 1)), coins, multiplier = Int.MAX_VALUE))
        assertEquals(4, f.count("obj.oak_logs"))
    }

    @Test
    fun `sale rolls back without output space and can use its freed source slot`() {
        val f = Fixture()
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

    @Test
    fun `sale cannot overflow the currency stack or sell an item the shop does not take`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.forestry_currency", Int.MAX_VALUE - 4)
        f.player.inv[1] = InvObj("obj.forestry_pheasant_feathers", 3)
        f.player.inv[2] = InvObj("obj.forestry_kit")
        f.sell("obj.forestry_pheasant_feathers", 3)
        assertEquals(Int.MAX_VALUE - 4, f.count("obj.forestry_currency"))
        assertEquals(3, f.count("obj.forestry_pheasant_feathers"))
        f.sell("obj.forestry_kit", 1)
        assertEquals(1, f.count("obj.forestry_kit"))
        f.sell("obj.forestry_pheasant_feathers", 0)
        assertEquals(3, f.count("obj.forestry_pheasant_feathers"))
    }

    private class Fixture {
        private val events = EventBus()
        private val coroutine = GameCoroutine("omnishop-test")
        private val script = OmnishopScript()
        private val shop = "dbrow.forestry_shop".asRSCM(RSCMType.DBROW)

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
        fun purchase(payments: List<OmnishopPayment>, output: ItemServerType, multiplier: Int = 1,
            requested: Int = 1, cert: Boolean = false) =
            buyOmnishopStock(player, payments, output, multiplier, requested, cert)
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
        private const val BLADE = 1
        private const val CAPE_POUCH = 6
        private const val PHEASANT_FEATHERS = 3
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
