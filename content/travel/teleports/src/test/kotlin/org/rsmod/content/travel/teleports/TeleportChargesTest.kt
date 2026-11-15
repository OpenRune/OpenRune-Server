package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.travel.teleports.TeleportCharges.spendCharge
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class TeleportChargesTest {
    @Test
    fun `a single-use item crumbles exactly the one in its slot`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(SEED_POD)
        f.player.inv[1] = InvObj(SEED_POD)
        assertTrue(f.run { spendCharge(f.item(SEED_POD), inv, 0) })
        assertNull(f.player.inv[0])
        assertTrue(f.player.inv[1].isType(SEED_POD))
        assertEquals(1, f.player.inv.count(SEED_POD))
    }

    @Test
    fun `an item that left its slot spends nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.coins", 10)
        assertFalse(f.run { spendCharge(f.item(SEED_POD), inv, 0) })
        assertFalse(f.run { spendCharge(f.item(SEED_POD), inv, 1) })
        assertEquals(10, f.player.inv.count("obj.coins"))

        f.player.inv[2] = InvObj(MEMOIRS)
        f.vars(5)
        assertFalse(f.run { spendCharge(f.item(MEMOIRS), inv, 0) })
        assertEquals(5, f.charges())
    }

    @Test
    fun `a varbit item spends one charge and refuses at zero`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MEMOIRS)
        f.vars(1)
        assertTrue(f.run { spendCharge(f.item(MEMOIRS), inv, 0) })
        assertEquals(0, f.charges())
        assertFalse(f.run { spendCharge(f.item(MEMOIRS), inv, 0) })
        assertEquals(0, f.charges())
        assertTrue(f.player.inv[0].isType(MEMOIRS))
    }

    @Test
    fun `reading a teleport scroll takes exactly one from its stack`() {
        val f = Fixture()
        f.player.inv[4] = InvObj(SCROLL, 5)
        assertTrue(f.run { TeleportScrolls.takeOne(this, SCROLL, 4) })
        assertEquals(4, f.player.inv.count(SCROLL))
        assertTrue(f.player.inv[4].isType(SCROLL))
    }

    @Test
    fun `the last teleport scroll empties its slot`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(SCROLL)
        assertTrue(f.run { TeleportScrolls.takeOne(this, SCROLL, 0) })
        assertNull(f.player.inv[0])
        assertEquals(0, f.player.inv.count(SCROLL))
    }

    @Test
    fun `a teleport scroll that moved or ran out takes nothing`() {
        val f = Fixture()
        f.player.inv[1] = InvObj(SCROLL, 2)
        f.player.inv[2] = InvObj("obj.coins", 10)
        assertFalse(f.run { TeleportScrolls.takeOne(this, SCROLL, 0) })
        assertFalse(f.run { TeleportScrolls.takeOne(this, SCROLL, 2) })
        assertEquals(2, f.player.inv.count(SCROLL))
        assertEquals(10, f.player.inv.count("obj.coins"))
        assertTrue(f.run { TeleportScrolls.takeOne(this, SCROLL, 1) })
        assertTrue(f.run { TeleportScrolls.takeOne(this, SCROLL, 1) })
        assertFalse(f.run { TeleportScrolls.takeOne(this, SCROLL, 1) })
        assertEquals(0, f.player.inv.count(SCROLL))
    }

    @Test
    fun `adding a memoirs page takes that page once and adds its charges`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MEMOIRS)
        f.player.inv[1] = InvObj(HOS_PAGE)
        f.player.inv[2] = InvObj(HOS_PAGE)
        assertTrue(f.run { with(Memoirs) { addPage(Memoirs.PAGES[0], MEMOIRS, 1) } })
        assertNull(f.player.inv[1])
        assertEquals(Memoirs.CHARGES_PER_PAGE, f.charges())
        assertFalse(f.run { with(Memoirs) { addPage(Memoirs.PAGES[0], MEMOIRS, 2) } })
        assertTrue(f.player.inv[2].isType(HOS_PAGE))
        assertEquals(Memoirs.CHARGES_PER_PAGE, f.charges())
    }

    @Test
    fun `a page that left its slot is not added`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MEMOIRS)
        f.player.inv[3] = InvObj("obj.coins", 5)
        assertFalse(f.run { with(Memoirs) { addPage(Memoirs.PAGES[1], MEMOIRS, 3) } })
        assertEquals(5, f.player.inv.count("obj.coins"))
        assertEquals(0, f.charges())
        assertFalse(f.run { with(Memoirs) { addPage(Memoirs.PAGES[1], MEMOIRS, 4) } })
        assertEquals(0, f.charges())
    }

    @Test
    fun `recharging takes one of each rune per charge up to the page cap`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MEMOIRS)
        f.player.inv[1] = InvObj(HOS_PAGE)
        f.run { with(Memoirs) { addPage(Memoirs.PAGES[0], MEMOIRS, 1) } }
        f.vars(5)
        Memoirs.RUNES.forEachIndexed { i, rune -> f.player.inv[2 + i] = InvObj(rune, 100) }
        f.player.inv[2] = InvObj(Memoirs.RUNES[0], 8)
        assertEquals(8, f.run { with(Memoirs) { recharge(MEMOIRS) } })
        assertEquals(13, f.charges())
        assertEquals(0, f.player.inv.count(Memoirs.RUNES[0]))
        for (rune in Memoirs.RUNES.drop(1)) {
            assertEquals(92, f.player.inv.count(rune))
        }

        f.player.inv[2] = InvObj(Memoirs.RUNES[0], 100)
        assertEquals(7, f.run { with(Memoirs) { recharge(MEMOIRS) } })
        assertEquals(Memoirs.CHARGES_PER_PAGE, f.charges())
        assertEquals(93, f.player.inv.count(Memoirs.RUNES[0]))
        assertEquals(-1, f.run { with(Memoirs) { recharge(MEMOIRS) } })
        assertEquals(93, f.player.inv.count(Memoirs.RUNES[0]))
    }

    @Test
    fun `recharging with a rune missing takes nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(MEMOIRS)
        f.player.inv[1] = InvObj(HOS_PAGE)
        f.run { with(Memoirs) { addPage(Memoirs.PAGES[0], MEMOIRS, 1) } }
        f.vars(0)
        Memoirs.RUNES.dropLast(1).forEachIndexed { i, rune ->
            f.player.inv[2 + i] = InvObj(rune, 50)
        }
        assertEquals(0, f.run { with(Memoirs) { recharge(MEMOIRS) } })
        assertEquals(0, f.charges())
        for (rune in Memoirs.RUNES.dropLast(1)) {
            assertEquals(50, f.player.inv.count(rune))
        }
    }

    private class Fixture {
        private val coroutine = GameCoroutine("teleport-charges-test")
        private val context =
            ProtectedAccessContextFactory.empty().copy(
                getEventBus = { EventBus() },
                getAlignment = { TextAlignment() },
            )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = SilentClient()
                uuid = 797L
                slotId = 1
                assignUid()
                coords = CoordGrid(3222, 3218, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv = Inventory(invType("inv.inv"), arrayOfNulls(28))
                worn = Inventory(invType("inv.worn"), arrayOfNulls(14))
            }

        private val access = ProtectedAccess(player, coroutine, context)

        fun item(obj: String): TeleportItem = checkNotNull(TeleportItems.byObj[obj]) { obj }

        fun <T> run(block: ProtectedAccess.() -> T): T {
            player.activeCoroutine = coroutine
            var result: T? = null
            val body: suspend () -> Unit = { result = access.block() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        result.getOrThrow()
                    }
                }
            )
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

        fun vars(charges: Int) {
            access.vars[CHARGES] = charges
        }

        fun charges(): Int = access.vars[CHARGES]
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
        private const val SEED_POD = "obj.aluft_seed_pod"
        private const val MEMOIRS = "obj.veos_kharedsts_memoirs"
        private const val CHARGES = "varbit.veos_memoir_charges"
        private const val SCROLL = "obj.teleportscroll_zulandra"
        private const val HOS_PAGE = "obj.veos_memoirs_hos_page"
        private val restored = mutableListOf<() -> Unit>()

        private fun invType(name: String) =
            checkNotNull(ServerCacheManager.getInventory(name.asRSCM()))

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field = Class.forName(owner).getDeclaredField(name)
                field.isAccessible = true
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
