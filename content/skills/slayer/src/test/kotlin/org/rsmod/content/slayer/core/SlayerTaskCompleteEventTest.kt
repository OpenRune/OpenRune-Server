package org.rsmod.content.slayer.core

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.player.events.PlayerSlayerEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.table.slayer.SlayerMastersRow
import org.rsmod.content.slayer.superior.SlayerSuperiorManager
import org.rsmod.events.EventBus
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class SlayerTaskCompleteEventTest {
    @Test
    fun `early in a streak points start at zero but the base is still reported`() {
        val f = Fixture(standardMaster())

        val event = f.finishTask()

        assertEquals(0, event.points)
        assertEquals(f.master.pointsPerTask, event.basePoints)
        assertEquals(1, event.streak)
        assertEquals(0, f.points())
    }

    @Test
    fun `a handler can pay points before the streak requirement`() {
        val f = Fixture(standardMaster())
        f.events.subscribeUnbound(PlayerSlayerEvents.TaskComplete::class.java) {
            points = basePoints * 5
        }

        f.finishTask()

        assertEquals(f.master.pointsPerTask * 5, f.points())
    }

    @Test
    fun `the fifth task pays natural points that a handler can replace`() {
        val f = Fixture(standardMaster())
        VarPlayerIntMapSetter.set(f.player, "varbit.slayer_tasks_completed", 4)

        val event = f.finishTask()

        assertEquals(f.master.pointsPerTask, event.points)
        assertEquals(f.master.pointsPerTask, f.points())
    }

    @Test
    fun `the task is still readable during the event and cleared after it`() {
        val f = Fixture(standardMaster())
        var targetDuring = 0
        f.events.subscribeUnbound(PlayerSlayerEvents.TaskComplete::class.java) {
            targetDuring = player.vars["varp.slayer_target"]
        }

        val event = f.finishTask()

        assertEquals(f.taskKey, targetDuring)
        assertNotNull(event.task)
        assertEquals(1, event.amount)
        assertEquals(0, f.player.vars["varp.slayer_target"])
        assertEquals(0, f.player.vars["varp.slayer_count"])
    }

    @Test
    fun `mortimer pays through his mortifier which is readable during the event`() {
        val f = Fixture(mortimer())
        VarPlayerIntMapSetter.set(f.player, "varbit.slayer_modifier_id", MortimerModifiers.MODIFIER_POINTS)
        VarPlayerIntMapSetter.set(f.player, "varbit.slayer_modifier_value", 5)
        var modifierDuring: Int? = null
        f.events.subscribeUnbound(PlayerSlayerEvents.TaskComplete::class.java) {
            modifierDuring = MortimerAssignment.activeModifier(player)?.id
        }

        val event = f.finishTask()

        assertEquals(MortimerModifiers.MODIFIER_POINTS, modifierDuring)
        assertEquals(5, event.points)
        assertEquals(0, event.basePoints)
        assertEquals(1, event.streak)
        assertEquals(5, f.points())
        assertEquals(1, MortimerAssignment.tasksCompleted(f.player))
        assertEquals(null, MortimerAssignment.activeModifier(f.player))
    }

    @Test
    fun `a kill not on task is not a task kill`() {
        val f = Fixture(standardMaster())
        assertTrue(SlayerTaskManager.isTaskKill(f.player, f.npc))

        VarPlayerIntMapSetter.set(f.player, "varp.slayer_target", 0)
        assertFalse(SlayerTaskManager.isTaskKill(f.player, f.npc))

        VarPlayerIntMapSetter.set(f.player, "varp.slayer_target", f.taskKey)
        VarPlayerIntMapSetter.set(f.player, "varp.slayer_count", 0)
        assertFalse(SlayerTaskManager.isTaskKill(f.player, f.npc))
    }

    @Test
    fun `the superior multiplier divides the denominator and never reaches zero`() {
        assertEquals(200, SlayerSuperiorManager.applyRateMultiplier(200, 0))
        assertEquals(200, SlayerSuperiorManager.applyRateMultiplier(200, 100))
        assertEquals(50, SlayerSuperiorManager.applyRateMultiplier(200, 400))
        assertEquals(45, SlayerSuperiorManager.applyRateMultiplier(180, 400))
        assertEquals(37, SlayerSuperiorManager.applyRateMultiplier(150, 400))
        assertEquals(400, SlayerSuperiorManager.applyRateMultiplier(200, 50))
        assertEquals(1, SlayerSuperiorManager.applyRateMultiplier(200, 1_000_000))
    }

    private fun standardMaster(): SlayerMastersRow =
        SlayerTaskManager.tasks.keys.first {
            it.pointsPerTask > 0 &&
                !MortimerAssignment.isMortimer(it) &&
                !SlayerTaskManager.isWildernessMaster(it)
        }

    private fun mortimer(): SlayerMastersRow =
        SlayerTaskManager.tasks.keys.first { MortimerAssignment.isMortimer(it) }

    private class Fixture(val master: SlayerMastersRow) {
        val npcType = checkNotNull(ServerCacheManager.getNpc("npc.araxyte_normal".asRSCM(RSCMType.NPC)))
        val npc = Npc(npcType, CoordGrid(0, 50, 50, 0, 0))
        val taskKey = SlayerTaskCredit.keys(npcType).first()
        val events = EventBus()
        val player =
            Player().apply {
                client = SilentClient()
                VarPlayerIntMapSetter.set(this, "varbit.slayer_master", master.masterId)
                VarPlayerIntMapSetter.set(this, "varp.slayer_target", taskKey)
                VarPlayerIntMapSetter.set(this, "varp.slayer_count", 1)
                VarPlayerIntMapSetter.set(this, "varp.slayer_count_original", 1)
            }

        fun points(): Int = player.vars["varbit.slayer_points"]

        fun finishTask(): PlayerSlayerEvents.TaskComplete {
            var seen: PlayerSlayerEvents.TaskComplete? = null
            events.subscribeUnbound(PlayerSlayerEvents.TaskComplete::class.java) { seen = this }
            assertTrue(SlayerTaskManager.decreaseTask(player, npc, events))
            return checkNotNull(seen)
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
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
