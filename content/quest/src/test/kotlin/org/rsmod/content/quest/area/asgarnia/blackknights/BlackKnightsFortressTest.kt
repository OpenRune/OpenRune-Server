package org.rsmod.content.quest.area.asgarnia.blackknights

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.*
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

@OptIn(InternalApi::class)
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class BlackKnightsFortressTest {
    private val p
        get() = BlackKnightsProgress

    @Test
    fun `twelve QP and inventory capacity gate handoff while reading is optional`() {
        val f = Fixture(0)
        VarPlayerIntMapSetter.set(f.player, "varp.qp", 11)
        assertFalse(p.dossier(f.access))
        assertEquals(0, f.stage)
        VarPlayerIntMapSetter.set(f.player, "varp.qp", 12)
        f.fill()
        assertFalse(p.dossier(f.access))
        f.player.inv[0] = null
        assertTrue(p.dossier(f.access))
        assertEquals(1, f.stage)
        assertEquals(1, f.player.inv.count(p.DOSSIER))
        VarPlayerIntMapSetter.set(f.player, "varp.qp", 11)
        assertTrue(p.read(f.access))
        assertEquals(1, f.stage)
        assertEquals(0, f.player.inv.count(p.DOSSIER))
        assertFalse(p.read(f.access))
    }

    @Test
    fun `complete mission accepts unread dossier consumes cabbage and pays exactly once`() {
        val f = Fixture(0)
        assertTrue(p.dossier(f.access))
        assertEquals(1, f.player.inv.count(p.DOSSIER))
        assertFalse(p.finish(f.access))
        f.player.coords = org.rsmod.map.CoordGrid(3025, 3507)
        assertTrue(p.hear(f.access))
        assertEquals(2, f.stage)
        f.give("obj.cabbage")
        f.player.coords = org.rsmod.map.CoordGrid(3031, 3508, 1)
        assertTrue(p.sabotage(f.access, "obj.cabbage"))
        assertEquals(0, f.player.inv.count("obj.cabbage"))
        assertEquals(3, f.stage)
        assertTrue(p.finish(f.access))
        assertEquals(4, f.stage)
        assertEquals(2500, f.player.inv.count("obj.coins"))
        assertEquals(1, f.player.inv.count(p.DOSSIER))
        assertEquals(15, f.player.vars["varp.qp"])
        val before = f.snapshot()
        assertFalse(p.finish(f.access))
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `wrong cabbage unearned knowledge wrong plane and remote hole preserve every item`() {
        val f = Fixture(1)
        f.give("obj.cabbage")
        f.player.coords = org.rsmod.map.CoordGrid(3031, 3508, 1)
        val before = f.snapshot()
        assertFalse(p.sabotage(f.access, "obj.cabbage"))
        assertEquals(before, f.snapshot())
        f.player.coords = org.rsmod.map.CoordGrid(3025, 3507)
        assertTrue(p.hear(f.access))
        val after = f.snapshot()
        assertFalse(p.sabotage(f.access, "obj.cabbage"))
        assertEquals(after, f.snapshot())
        val draynor = checkNotNull(ServerCacheManager.getItem("obj.magic_cabbage".asRSCM()))
        f.give(draynor.internalName)
        f.player.coords = org.rsmod.map.CoordGrid(3031, 3508, 1)
        val wrong = f.snapshot()
        assertFalse(p.sabotage(f.access, draynor.internalName))
        assertEquals(wrong, f.snapshot())
        assertFalse(p.hear(f.access))
    }

    @Test
    fun `full bag final reward is retryable and an existing coin stack needs no extra slot`() {
        val f = Fixture(3)
        f.fill()
        val before = f.snapshot()
        assertFalse(p.finish(f.access))
        assertEquals(before, f.snapshot())
        f.player.inv[0] = null
        assertTrue(p.finish(f.access))
        assertEquals(2500, f.player.inv.count("obj.coins"))
        assertEquals(15, f.player.vars["varp.qp"])
        val g = Fixture(3)
        g.fill()
        g.player.inv[4] = InvObj("obj.coins", 10)
        assertTrue(p.finish(g.access))
        assertEquals(2510, g.player.inv.count("obj.coins"))
        assertFalse(p.finish(g.access))
    }

    @Test
    fun `lost dossier replacement guards permanent bank and reread cannot advance partial quest`() {
        val f = Fixture(0)
        assertTrue(p.dossier(f.access))
        assertFalse(p.dossier(f.access))
        f.player.inv[0] = null
        assertTrue(p.dossier(f.access))
        f.player.inv[0] = null
        f.player.invMap.getOrPut("inv.bank")[0] = InvObj(p.DOSSIER)
        assertFalse(p.dossier(f.access))
        f.player.invMap.getOrPut("inv.bank")[0] = null
        f.setStage(2)
        assertTrue(p.dossier(f.access))
        assertTrue(p.read(f.access))
        assertEquals(2, f.stage)
        assertEquals(12, f.player.vars["varp.qp"])
    }

    @Test
    fun `production saved spy stage survives reload and twelve QP is a start gate only`() {
        val f = Fixture(2)
        val saved = f.player.vars.backing.toMap()
        val reloaded = Fixture(0)
        reloaded.player.vars.backing.putAll(saved)
        assertEquals(2, reloaded.stage)
        assertEquals(2, reloaded.player.vars["varp.spy"])
        assertFalse(reloaded.script.quest.isQuestCompleted(reloaded.player))
        reloaded.setStage(3)
        VarPlayerIntMapSetter.set(reloaded.player, "varp.qp", 0)
        assertTrue(p.finish(reloaded.access))
        assertEquals(3, reloaded.player.vars["varp.qp"])
        assertEquals(4, reloaded.player.vars["varp.spy"])
    }

    @Test
    fun `overflowing coin reward preserves earned sabotage and retries after making room`() {
        val f = Fixture(3)
        f.give("obj.coins", Int.MAX_VALUE - 2499)
        val before = f.snapshot()
        assertFalse(p.finish(f.access))
        assertEquals(before, f.snapshot())
        f.player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE - 2500)
        assertTrue(p.finish(f.access))
        assertEquals(Int.MAX_VALUE, f.player.inv.count("obj.coins"))
        val completed = f.snapshot()
        assertFalse(p.finish(f.access))
        assertEquals(completed, f.snapshot())
    }

    private class Fixture(stage: Int) {
        val script = BlackKnightsFortress()

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                client = RecordingClient()
                uuid = 104L
                slotId = 1
                assignUid()
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                worn =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                        arrayOfNulls(14),
                    )
            }
        val access =
            ProtectedAccess(
                player,
                GameCoroutine("black-knights-test"),
                ProtectedAccessContextFactory.empty().copy(getEventBus = { EventBus() }),
            )

        init {
            setStage(stage)
            VarPlayerIntMapSetter.set(player, "varp.qp", 12)
        }

        val stage
            get() = script.quest.getQuestStage(player)

        fun setStage(stage: Int) =
            VarPlayerIntMapSetter.set(player, "varbit.black_knights_progress", stage)

        fun give(item: String, count: Int = 1) {
            val t = checkNotNull(ServerCacheManager.getItem(item.asRSCM()))
            if (t.stackable) {
                val at =
                    player.inv.objs.indexOfFirst { it?.id == t.id }.takeIf { it >= 0 }
                        ?: player.inv.objs.indexOfFirst { it == null }
                player.inv[at] = InvObj(item, player.inv.count(item) + count)
            } else
                repeat(count) {
                    player.inv[player.inv.objs.indexOfFirst { it == null }] = InvObj(item)
                }
        }

        fun fill() {
            for (i in 0 until 28) player.inv[i] = InvObj("obj.black_bead", 1)
        }

        fun snapshot() =
            listOf(
                player.inv.objs.toList(),
                stage,
                player.vars["varp.qp"],
                player.statMap.getXP("stat.crafting"),
            )
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private val restored = mutableListOf<() -> Unit>()

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
                val field =
                    Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
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
