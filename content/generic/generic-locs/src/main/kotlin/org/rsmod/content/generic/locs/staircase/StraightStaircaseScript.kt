package org.rsmod.content.generic.locs.staircase

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Landing offsets for straight house staircases, measured on an unrotated instance of each model and
 * rotated with the loc. The narrow wooden flight is modelled facing the opposite way.
 */
private enum class StraightStairs(val loc: String, val dx: Int, val dz: Int, val dLevel: Int) {
    PoorUp("loc.poor_stairs", 1, 3, 1),
    PoorDown("loc.poor_stairstop", 1, -2, -1),
    Up("loc.stairs", 1, 3, 1),
    Down("loc.stairstop", 1, -2, -1),
    NarrowWoodenUp("loc.narrowstairs_wooden_bottom", 0, -1, 1),
    NarrowWoodenDown("loc.narrowstairs_wooden_top", 0, 3, -1),
}

class StraightStaircaseScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (stairs in StraightStairs.entries) {
            onOpLoc1(stairs.loc) { climb(it.loc, stairs) }
        }
    }

    private suspend fun ProtectedAccess.climb(loc: BoundLocInfo, stairs: StraightStairs) {
        arriveDelay()
        telejump(loc.rotate(stairs.dx, stairs.dz).translateLevel(stairs.dLevel))
    }

    private fun BoundLocInfo.rotate(dx: Int, dz: Int): CoordGrid =
        when (angle) {
            LocAngle.West -> coords.translate(dx, dz)
            LocAngle.North -> coords.translate(dz, width - 1 - dx)
            LocAngle.East -> coords.translate(width - 1 - dx, length - 1 - dz)
            LocAngle.South -> coords.translate(length - 1 - dz, dx)
        }
}
