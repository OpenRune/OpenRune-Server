package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class FossilUnderwaterObstaclesTest {
    /** Places [loc] as the map has it; the test world loads no locs on the sea floor. */
    private fun GameTestScope.mapLoc(loc: String, coords: CoordGrid, angle: Int): BoundLocInfo {
        placeMapLoc(coords, loc, LocShape.CentrepieceStraight, LocAngle[angle])
        val type = checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))
        val info = findLocs(coords).first { it.coords == coords && it.id == type.id }
        return BoundLocInfo(info, type)
    }

    @Test
    fun GameTestState.`a fishbowl is turned back from the deeper water`() = runShortcutTest {
        setAgility(1)
        startAt(CoordGrid(3764, 10282, 1))
        player.inv[0] = InvObj(FISHBOWL)
        val before = agilityXp()

        player.opLoc1(mapLoc(TUNNEL, CoordGrid(3764, 10281, 1), angle = 0))
        advance(4)

        assertEquals(CoordGrid(3764, 10282, 1), player.coords)
        assertEquals(before, agilityXp())
        assertTrue(player.ui.containsModal("interface.chat_left"))
    }

    private companion object {
        const val TUNNEL = "loc.fossil_underwater_deepwater_agihole"
        const val FISHBOWL = "obj.hundred_pirate_diving_helmet"
    }
}
