package org.rsmod.content.areas.city.rimmington

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The houses around Rimmington share one straight staircase model, so both ends carry a single
 * offset measured on an unrotated instance: climbing up ends at the far end of the flight, and
 * climbing down two tiles back the other way.
 */
private enum class PoorStairs(val loc: String, val dx: Int, val dz: Int, val dLevel: Int) {
    Up("loc.poor_stairs", 1, 3, 1),
    Down("loc.poor_stairstop", 1, -2, -1),
}

class RimmingtonLocScript @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        for (stairs in PoorStairs.entries) {
            onOpLoc1(stairs.loc) { climb(it.loc, stairs) }
        }

        onOpLoc1(WARDROBE_SHUT) { openWardrobe(it.loc) }
        onOpLoc3(WARDROBE_OPEN) { shutWardrobe(it.loc) }
        onOpLoc2(WARDROBE_OPEN) { mes("The wardrobe is empty.") }
    }

    private suspend fun ProtectedAccess.climb(loc: BoundLocInfo, stairs: PoorStairs) {
        arriveDelay()
        telejump(loc.rotate(stairs.dx, stairs.dz).translateLevel(stairs.dLevel))
    }

    private suspend fun ProtectedAccess.openWardrobe(wardrobe: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_openbigcupboard")
        soundSynth("synth.wardrobe_open")
        delay(1)
        locRepo.change(wardrobe, WARDROBE_OPEN, WARDROBE_DURATION)
    }

    private suspend fun ProtectedAccess.shutWardrobe(wardrobe: BoundLocInfo) {
        arriveDelay()
        anim("seq.human_closechest")
        soundSynth("synth.wardrobe_close")
        delay(1)
        locRepo.change(wardrobe, WARDROBE_SHUT, WARDROBE_DURATION)
    }

    private fun BoundLocInfo.rotate(dx: Int, dz: Int): CoordGrid =
        when (angle) {
            LocAngle.West -> coords.translate(dx, dz)
            LocAngle.North -> coords.translate(dz, width - 1 - dx)
            LocAngle.East -> coords.translate(width - 1 - dx, length - 1 - dz)
            LocAngle.South -> coords.translate(length - 1 - dz, dx)
        }

    private companion object {
        const val WARDROBE_SHUT = "loc.spookywardrobe"
        const val WARDROBE_OPEN = "loc.spookywardrobe_open"
        const val WARDROBE_DURATION = 500
    }
}
