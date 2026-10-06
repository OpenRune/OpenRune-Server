package org.rsmod.api.game.process.player

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.setActiveMoveSpeed
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.movement.MoveSpeed

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class RunAutoEnableTest {
    private val processor = PlayerRunUpdateProcessor()

    @Test
    fun `run is re-enabled when restored energy climbs past the threshold`() {
        val player = player(threshold = 50, energy = 4_990)
        processor.process(player)
        assertTrue(ToggleQueue in player.queueList)
    }

    @Test
    fun `energy already above the threshold does not force run back on`() {
        val player = player(threshold = 50, energy = 6_000)
        processor.process(player)
        assertFalse(ToggleQueue in player.queueList)
    }

    @Test
    fun `a zero threshold leaves the setting off`() {
        val player = player(threshold = 0, energy = 4_990)
        processor.process(player)
        assertFalse(ToggleQueue in player.queueList)
    }

    @Test
    fun `players already running are left alone`() {
        val player = player(threshold = 50, energy = 4_990)
        player.setActiveMoveSpeed(MoveSpeed.Run)
        processor.process(player)
        assertFalse(ToggleQueue in player.queueList)
    }

    private fun player(threshold: Int, energy: Int): Player =
        Player().apply {
            runEnergy = energy
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
