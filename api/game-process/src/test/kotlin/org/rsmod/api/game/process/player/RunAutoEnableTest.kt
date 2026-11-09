package org.rsmod.api.game.process.player

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.autoEnableRunOnMove
import org.rsmod.api.player.vars.setActiveMoveSpeed
import org.rsmod.api.player.vars.varMoveSpeed
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.movement.MoveSpeed

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class RunAutoEnableTest {
    private val processor = PlayerRunUpdateProcessor()

    @Test
    fun `energy climbing past the threshold while standing leaves run off`() {
        val player = depleted(threshold = 50)
        player.runEnergy = 4_990
        processor.process(player)

        assertEquals(MoveSpeed.Walk, player.varMoveSpeed)
        assertFalse(ToggleQueue in player.queueList)
    }

    @Test
    fun `a move request at or above the threshold turns run back on after running out`() {
        val player = depleted(threshold = 50)
        player.runEnergy = 5_000
        player.autoEnableRunOnMove()

        assertEquals(MoveSpeed.Run, player.varMoveSpeed)
        assertEquals(MoveSpeed.Run, player.moveSpeed)
    }

    @Test
    fun `a move request below the threshold keeps walking until energy reaches it`() {
        val player = depleted(threshold = 50)
        player.runEnergy = 4_999
        player.autoEnableRunOnMove()
        assertEquals(MoveSpeed.Walk, player.varMoveSpeed)

        player.runEnergy = 5_000
        player.autoEnableRunOnMove()
        assertEquals(MoveSpeed.Run, player.varMoveSpeed)
    }

    @Test
    fun `run is re-enabled once per depletion`() {
        val player = depleted(threshold = 50)
        player.runEnergy = 6_000
        player.autoEnableRunOnMove()
        player.setActiveMoveSpeed(MoveSpeed.Walk)

        player.autoEnableRunOnMove()

        assertEquals(MoveSpeed.Walk, player.varMoveSpeed)
    }

    @Test
    fun `walking by choice is never overridden`() {
        val player = player(threshold = 50)
        player.runEnergy = 9_000

        player.autoEnableRunOnMove()

        assertEquals(MoveSpeed.Walk, player.varMoveSpeed)
    }

    @Test
    fun `running out during a one-off run while walk is selected does not re-enable run`() {
        val player = player(threshold = 50)
        player.moveSpeed = MoveSpeed.Run
        player.runEnergy = 1
        player.pendingStepCount = 2
        processor.process(player)
        player.pendingStepCount = 0
        player.runEnergy = 6_000

        player.autoEnableRunOnMove()

        assertEquals(MoveSpeed.Walk, player.varMoveSpeed)
    }

    @Test
    fun `a zero threshold leaves the setting off`() {
        val player = depleted(threshold = 0)
        player.runEnergy = 9_000

        player.autoEnableRunOnMove()

        assertEquals(MoveSpeed.Walk, player.varMoveSpeed)
    }

    private fun depleted(threshold: Int): Player =
        player(threshold).apply {
            setActiveMoveSpeed(MoveSpeed.Run)
            runEnergy = 1
            pendingStepCount = 2
            processor.process(this)
            pendingStepCount = 0
            check(runEnergy == 0 && varMoveSpeed == MoveSpeed.Walk)
        }

    private fun player(threshold: Int): Player =
        Player().apply {
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            VarPlayerIntMapSetter.set(this, "varbit.runenergy_autoenable", threshold)
        }

    companion object {
        private const val ToggleQueue = "queue.runmode_toggle"

        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
        }
    }
}
