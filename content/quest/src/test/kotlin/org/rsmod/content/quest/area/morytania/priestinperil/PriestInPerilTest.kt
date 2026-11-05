package org.rsmod.content.quest.area.morytania.priestinperil

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.hunt.HuntType
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
import org.rsmod.api.player.ui.ifClose
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.quest.manager.QuestRequirementMode
import org.rsmod.content.quest.manager.QuestRequirementPolicy
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
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
class PriestInPerilTest {
    @Test
    fun `cached monk hunt cannot bypass the quest aggression conditions`() {
        for (type in
            listOf(
                "npc.priestperilevilmonk1",
                "npc.priestperilevilmonk2",
                "npc.priestperilevilmonk3",
                "npc.priestperilevilmonk1_vis",
                "npc.priestperilevilmonk2_vis",
                "npc.priestperilevilmonk3_vis",
            )) {
            val npc = checkNotNull(ServerCacheManager.getNpc(type.asRSCM()))
            val hunt = npc.huntMode?.let { ServerCacheManager.getHunt(it) }
            assertTrue(npc.huntRange == 0 || hunt == null || hunt.type == HuntType.Off, type)
            assertEquals(1, npc.attackRange, type)
        }
    }

    @Test
    fun `golden key belongs only to the original hooded monk while a replacement is needed`() {
        val f = Fixture(5)
        val spawn = CoordGrid(3411, 3489, 0)
        val monk = Npc("npc.priestperilevilmonk3", spawn)
        assertTrue(f.script.dropsGoldenKey(f.player, monk))
        monk.coords = CoordGrid(3400, 3500, 0)
        assertTrue(f.script.dropsGoldenKey(f.player, monk))
        for (type in
            listOf("npc.priestperilevilmonk1", "npc.priestperilevilmonk2", "npc.chaosmonk1")) {
            assertFalse(f.script.dropsGoldenKey(f.player, Npc(type, spawn)))
        }
        assertFalse(
            f.script.dropsGoldenKey(
                f.player,
                Npc("npc.priestperilevilmonk3", CoordGrid(3415, 3485, 0)),
            )
        )
        for (stage in listOf(0, 1, 2, 3, 6, 7, 8, 60)) {
            f.setStage(stage)
            assertFalse(f.script.dropsGoldenKey(f.player, monk))
        }
        f.setStage(4)
        assertTrue(f.script.dropsGoldenKey(f.player, monk))
        f.player.inv[0] = InvObj(PriestInPeril.GOLD_KEY, 1)
        assertFalse(f.script.dropsGoldenKey(f.player, monk))
        f.player.inv[0] = null
        f.player.invMap.getOrPut("inv.bank")[0] = InvObj(PriestInPeril.IRON_KEY, 1)
        assertFalse(f.script.dropsGoldenKey(f.player, monk))
    }

    @Test
    fun `only the hooded monk is aggressive before the cell is unlocked`() {
        val f = Fixture(0)
        for (stage in 0..5) {
            f.setStage(stage)
            assertTrue(f.script.canMonkAggress(f.player, true))
            assertFalse(f.script.canMonkAggress(f.player, false))
        }
        f.setStage(6)
        assertTrue(f.script.canMonkAggress(f.player, false))
        VarPlayerIntMapSetter.set(f.player, "varbit.priestperilevilmonk_dead", 1)
        assertFalse(f.script.canMonkAggress(f.player, true))
    }

    @Test
    fun `hooded monk remains hidden for owned keys and recovers after actual key loss`() {
        val f = Fixture(5)
        assertFalse(f.script.keepMonkHidden(f.player, false))
        assertTrue(f.script.keepMonkHidden(f.player, true))
        for (key in listOf(PriestInPeril.GOLD_KEY, PriestInPeril.IRON_KEY)) {
            f.player.inv[0] = InvObj(key, 1)
            assertTrue(PriestInPeril().keepMonkHidden(f.player, false))
            f.player.inv[0] = null
            val bank = f.player.invMap.getOrPut("inv.bank")
            bank[0] = InvObj(key, 1)
            assertTrue(PriestInPeril().keepMonkHidden(f.player, false))
            bank[0] = null
            assertFalse(f.script.keepMonkHidden(f.player, false))
        }
        f.setStage(6)
        assertTrue(f.script.keepMonkHidden(f.player, false))
    }

    @Test
    fun `mausoleum requires a real golden key until its persistent unlock`() {
        val f = Fixture(4)
        assertFalse(f.script.canEnterMausoleum(f.player))
        f.player.inv[0] = InvObj(PriestInPeril.GOLD_KEY, 1)
        assertTrue(f.script.canEnterMausoleum(f.player))
        VarPlayerIntMapSetter.set(f.player, PriestInPeril.MAUSOLEUM_UNLOCKED, 1)
        f.player.inv[0] = null
        assertTrue(PriestInPeril().canEnterMausoleum(f.player))
        f.setStage(0)
        assertFalse(f.script.canEnterMausoleum(f.player))
    }

    @Test
    fun `mixed essence hand-ins consume only exact outstanding unnoted count`() {
        val f = Fixture(10)
        for (i in 0..9) f.player.inv[i] = InvObj(PriestInPeril.RUNE_ESSENCE, 1)
        for (i in 10..19) f.player.inv[i] = InvObj(PriestInPeril.PURE_ESSENCE, 1)
        f.player.inv[20] = InvObj("obj.cert_blankrune", 500)
        assertEquals(20, f.script.donateEssence(f.access))
        assertEquals(30, f.stage)
        assertEquals(500, f.player.inv.count("obj.cert_blankrune"))
        assertEquals(0, f.player.statMap.getXP("stat.prayer"))
        for (i in 0..27) f.player.inv[i] = InvObj(PriestInPeril.PURE_ESSENCE, 1)
        assertEquals(28, f.script.donateEssence(f.access))
        assertEquals(58, f.stage)
        for (i in 0..4) f.player.inv[i] = InvObj(PriestInPeril.RUNE_ESSENCE, 1)
        assertEquals(2, f.script.donateEssence(f.access))
        assertEquals(59, f.stage)
        assertEquals(3, f.player.inv.count(PriestInPeril.RUNE_ESSENCE))
        assertEquals(0, f.script.donateEssence(f.access))
    }

    @Test
    fun `notes and banked essence cannot substitute for unnoted hand-ins`() {
        val f = Fixture(10)
        f.player.inv[0] = InvObj("obj.cert_blankrune", 50)
        f.player.inv[1] = InvObj("obj.cert_blankrune_high", 50)
        f.player.invMap.getOrPut("inv.bank")[0] = InvObj(PriestInPeril.RUNE_ESSENCE, 50)
        val before = f.snapshot()
        assertEquals(0, f.script.donateEssence(f.access))
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `wrong stages and stale inputs never consume essence or finish`() {
        for (stage in 0..9) {
            val f = Fixture(stage)
            f.player.inv[0] = InvObj(PriestInPeril.RUNE_ESSENCE, 1)
            val before = f.snapshot()
            assertEquals(0, f.script.donateEssence(f.access))
            assertFalse(f.script.finish(f.access))
            assertEquals(before, f.snapshot())
        }
        val f = Fixture(10)
        assertEquals(0, f.script.donateEssence(f.access))
        assertFalse(f.script.finish(f.access))
    }

    @Test
    fun `reward grants exactly once and completion alone does not waive blessing`() {
        val f = Fixture(59, 50)
        assertFalse(QuestRequirements.hasCompleted(f.player, "quest_priestinperil"))
        assertFalse(f.script.canPass(f.player))
        assertTrue(f.script.finish(f.access))
        assertEquals(60, f.stage)
        assertEquals(1, f.player.vars["varp.qp"])
        assertEquals(1406, f.player.statMap.getXP("stat.prayer"))
        assertEquals(1, f.player.inv.count(PriestInPeril.WOLFBANE))
        assertTrue(QuestRequirements.hasCompleted(f.player, "quest_priestinperil"))
        assertFalse(f.script.canPass(f.player))
        VarPlayerIntMapSetter.set(f.player, PriestInPeril.SALVE_BLESSING, 1)
        assertTrue(f.script.canPass(f.player))
        val before = f.snapshot()
        assertFalse(f.script.finish(f.access))
        assertEquals(0, f.script.donateEssence(f.access))
        assertEquals(before, f.snapshot())
    }

    @Test
    fun `full reward inventory retains earned hand-in and permits retry without extra fee`() {
        val f = Fixture(59, 50)
        f.fill()
        val before = f.snapshot()
        assertFalse(f.script.finish(f.access))
        assertEquals(before, f.snapshot())
        f.player.inv[27] = null
        assertTrue(f.script.finish(f.access))
        assertEquals(27, f.player.inv.count("obj.black_bead"))
        assertEquals(1, f.player.inv.count(PriestInPeril.WOLFBANE))
        assertEquals(1406, f.player.statMap.getXP("stat.prayer"))
    }

    @Test
    fun `key and bucket exchanges conserve one item and reject missing inputs`() {
        for ((from, to) in
            listOf(
                PriestInPeril.GOLD_KEY to PriestInPeril.IRON_KEY,
                "obj.bucket_empty" to PriestInPeril.MURKY_WATER,
                PriestInPeril.MURKY_WATER to PriestInPeril.BLESSED_WATER,
                PriestInPeril.BLESSED_WATER to "obj.bucket_empty",
            )) {
            val f = Fixture(5)
            f.fill()
            var before = f.snapshot()
            assertFalse(f.script.exchange(f.access, from, to))
            assertEquals(before, f.snapshot())
            f.player.inv[5] = InvObj(from, 1)
            assertTrue(f.script.exchange(f.access, from, to))
            assertEquals(0, f.player.inv.count(from))
            assertEquals(1, f.player.inv.count(to))
            assertEquals(27, f.player.inv.count("obj.black_bead"))
            before = f.snapshot()
            assertFalse(f.script.exchange(f.access, from, to))
            assertEquals(before, f.snapshot())
        }
    }

    @Test
    fun `partial donation and reward state survive a fresh quest script`() {
        val f = Fixture(10)
        for (i in 0..24) f.player.inv[i] = InvObj(PriestInPeril.RUNE_ESSENCE, 1)
        assertEquals(25, f.script.donateEssence(f.access))
        val reload = PriestInPeril()
        assertEquals(35, reload.stage(f.player))
        assertEquals(35, f.player.vars["varp.priestperil"])
        assertFalse(QuestRequirements.hasCompleted(f.player, "quest_priestinperil"))
        for (i in 0..24) f.player.inv[i] = InvObj(PriestInPeril.PURE_ESSENCE, 1)
        assertEquals(25, reload.donateEssence(f.access))
        assertTrue(reload.finish(f.access))
        assertEquals(60, f.script.stage(f.player))
    }

    @Test
    fun `raw native stage or blessing without real quest progress never opens Morytania`() {
        val f = Fixture(0)
        VarPlayerIntMapSetter.set(f.player, "varp.priestperil", 60)
        VarPlayerIntMapSetter.set(f.player, PriestInPeril.SALVE_BLESSING, 1)
        assertFalse(QuestRequirements.hasCompleted(f.player, "quest_priestinperil"))
        assertFalse(f.script.canPass(f.player))
    }

    @Test
    fun `registered holy barrier requires membership completion and the separate blessing`() {
        for (completed in listOf(false, true)) for (blessed in listOf(false, true)) for (members in
            listOf(false, true)) {
            val f = Fixture(if (completed) 60 else 0)
            f.player.members = members
            f.player.coords = CoordGrid(3440, 9887)
            VarPlayerIntMapSetter.set(f.player, PriestInPeril.SALVE_BLESSING, if (blessed) 1 else 0)
            val events = EventBus()
            f.player.ifClose(events)
            val script =
                PriestInPerilTravel(
                    f.script,
                    io.mockk.mockk(),
                    io.mockk.mockk(),
                    org.rsmod.game.MapClock(100),
                )
            with(script) { ScriptContext(events, CheatCommandMap(), EngineQueueCache()).startup() }
            val type =
                checkNotNull(
                    ServerCacheManager.getObject(
                        "loc.pip_underground_wall_side_withportal".asRSCM()
                    )
                )
            val loc =
                org.rsmod.game.loc.BoundLocInfo(
                    org.rsmod.game.loc.LocInfo(
                        0,
                        f.player.coords,
                        org.rsmod.game.loc.LocEntity(type.id, 0, 0),
                    ),
                    type,
                )
            val collision =
                org.rsmod.routefinder.collision.CollisionFlagMap().apply {
                    allocateIfAbsent(f.player.coords.x, f.player.coords.z, 0)
                    allocateIfAbsent(
                        PriestInPerilTravel.MORYTANIA_EXIT.x,
                        PriestInPerilTravel.MORYTANIA_EXIT.z,
                        0,
                    )
                }
            val context =
                ProtectedAccessContextFactory.empty()
                    .copy(
                        getEventBus = { events },
                        getCollision = { collision },
                        getAlignment = { org.rsmod.api.player.dialogue.align.TextAlignment() },
                        getTeleportValidator = {
                            org.rsmod.api.player.hook.PlayerTeleportValidator(emptySet())
                        },
                        getAreaChecker = { io.mockk.mockk(relaxed = true) },
                    )
            val before = f.snapshot()
            assertTrue(
                org.rsmod.api.player.protect.ProtectedAccessLauncher.withProtectedAccess(
                    f.player,
                    context,
                ) {
                    assertTrue(
                        events.publish(
                            this,
                            org.rsmod.api.player.events.interact.LocEvents.Op1(loc, loc, type),
                        )
                    )
                },
                "launch complete=$completed blessed=$blessed member=$members protected=${f.player.isAccessProtected}",
            )
            repeat(5) {
                f.player.currentMapClock++
                f.player.processedMapClock = f.player.currentMapClock
                if (f.player.isNotDelayed) f.player.advanceActiveCoroutine()
            }
            assertEquals(
                if (completed && blessed && members) PriestInPerilTravel.MORYTANIA_EXIT
                else CoordGrid(3440, 9887),
                f.player.coords,
                "complete=$completed blessed=$blessed member=$members",
            )
            assertEquals(before, f.snapshot())
        }
    }

    private class Fixture(stage: Int, donated: Int = (stage - 10).coerceIn(0, 50)) {
        val script = PriestInPeril()

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
                GameCoroutine("priest-in-peril-test"),
                ProtectedAccessContextFactory.empty().copy(getEventBus = { EventBus() }),
            )

        init {
            setStage(stage)
            VarPlayerIntMapSetter.set(player, PriestInPeril.ESSENCE_COUNT, donated)
        }

        val stage
            get() = script.quest.getQuestStage(player)

        fun setStage(stage: Int) {
            script.quest.setQuestStage(access, stage)
        }

        fun fill() {
            for (i in 0 until 28) player.inv[i] = InvObj("obj.black_bead", 1)
        }

        fun snapshot() =
            listOf(
                player.inv.objs.toList(),
                stage,
                player.vars["varp.qp"],
                player.statMap.getXP("stat.prayer"),
                script.donatedEssence(player),
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
            val oldPolicy = QuestRequirements.activePolicy()
            restored += { QuestRequirements.install(oldPolicy) }
            QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
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
