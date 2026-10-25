package org.rsmod.content.events.randomevents

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.content.events.randomevents.GenieRandomEvent.Companion.CALL_INTERVAL
import org.rsmod.content.events.randomevents.GenieRandomEvent.Companion.LIFETIME_TICKS
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class GenieRandomEventTest {
    @Test
    fun GameTestState.`the genie spawns beside its owner and faces them`() =
        runInjectedGameTest(GenieRandomEvent::class, null, GenieRandomEvent::class) { script ->
            val center = CoordGrid(0, 50, 50, 20, 20)
            allocZoneCollision(center)
            player.teleport(center)
            val genie = script.spawn(player)
            assertEquals(1, genie.coords.chebyshevDistance(player.coords))
            assertTrue(genie.faceEntity.isPlayer)
            assertEquals(player.slotId, genie.faceEntity.playerSlot)
            assertEquals(genie, script.genieOf(player))
        }

    @Test
    fun GameTestState.`an ignored genie leaves after a minute`() =
        runInjectedGameTest(GenieRandomEvent::class, null, GenieRandomEvent::class) { script ->
            val center = CoordGrid(0, 50, 50, 20, 20)
            allocZoneCollision(center)
            player.teleport(center)
            val genie = script.spawn(player)
            advance(ticks = LIFETIME_TICKS + CALL_INTERVAL)
            assertEquals(null, script.genieOf(player))
            assertFalse(genie.isSlotAssigned)
        }
}
