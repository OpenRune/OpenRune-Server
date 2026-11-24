package org.rsmod.content.areas.misc.corsaircove

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CorsairCoveScenery : PluginScript() {
    override fun ScriptContext.startup() {
        // Shared loc types must be registered once; dispatch placement differences through the table.
        onOpLoc1(StairsUp) { climbStairs(it.loc, STAIRS_UP) }
        onOpLoc1(StairsDown) { climbStairs(it.loc, STAIRS_DOWN) }
        onOpLoc1(StairsRamp) { climbStairs(it.loc, RAMPS) }

        onOpLoc1(VineLadderEntrance) { enterHole(it.loc) }
        onOpLoc1(VineLadder) { climbVineLadder(it.loc) }
    }

    private suspend fun ProtectedAccess.climbStairs(
        loc: BoundLocInfo,
        flights: Map<CoordGrid, CoordGrid>,
    ) {
        val destination = flights[loc.column()] ?: return
        arriveDelay()
        telejump(destination)
    }

    // Preserve the approach row when translating between the surface hole and underground vines.
    private suspend fun ProtectedAccess.enterHole(loc: BoundLocInfo) {
        if (loc.column() != HoleColumn) {
            return
        }
        arriveDelay()
        anim(HoleAnim)
        delay(1)
        telejump(CaveBottom.shiftedZ(rowOffset(loc)))
    }

    private suspend fun ProtectedAccess.climbVineLadder(loc: BoundLocInfo) {
        if (loc.column() != VineLadderColumn) {
            return
        }
        arriveDelay()
        anim(LadderAnim)
        delay(1)
        telejump(CaveTop.shiftedZ(rowOffset(loc)))
    }

    private fun ProtectedAccess.rowOffset(loc: BoundLocInfo): Int =
        (player.coords.z - loc.coords.z).coerceIn(-1, 1)

    private companion object {
        private const val StairsUp = "loc.ds2_corsair_cove_stairs"

        private const val StairsDown = "loc.ds2_corsair_cove_stairs_down"

        private const val StairsRamp = "loc.ds2_corsair_cove_stairs_ramp"

        private const val VineLadderEntrance = "loc.ds2_ogre_corsair_vine_ladder_entrance"

        private const val VineLadder = "loc.ds2_ogre_corsair_vine_ladder"

        private const val HoleAnim = "seq.human_pickupfloor"

        private const val LadderAnim = "seq.human_reachforladder"

        private val STAIRS_UP: Map<CoordGrid, CoordGrid> =
            mapOf(
                column(2555, 2855) to CoordGrid(2555, 2854, 1),
                column(2548, 2862) to CoordGrid(2547, 2862, 1),
                column(2561, 2866) to CoordGrid(2561, 2865, 1),
            )

        private val STAIRS_DOWN: Map<CoordGrid, CoordGrid> =
            mapOf(
                column(2555, 2855) to CoordGrid(2555, 2857, 0),
                column(2548, 2862) to CoordGrid(2550, 2862, 0),
                column(2561, 2867) to CoordGrid(2561, 2869, 0),
            )

        private val RAMPS: Map<CoordGrid, CoordGrid> =
            mapOf(
                column(2531, 2833) to CoordGrid(2529, 2835, 1),
                column(2529, 2834) to CoordGrid(2532, 2833, 0),
            )

        private val HoleColumn: CoordGrid = column(2523, 2861)
        private val VineLadderColumn: CoordGrid = column(2012, 9005)

        private val CaveBottom: CoordGrid = CoordGrid(2012, 9005, 1)

        private val CaveTop: CoordGrid = CoordGrid(2523, 2861, 0)

        private fun column(x: Int, z: Int): CoordGrid = CoordGrid(x, z, 0)

        private fun BoundLocInfo.column(): CoordGrid = column(coords.x, coords.z)

        private fun CoordGrid.shiftedZ(offset: Int): CoordGrid = CoordGrid(x, z + offset, level)
    }
}
