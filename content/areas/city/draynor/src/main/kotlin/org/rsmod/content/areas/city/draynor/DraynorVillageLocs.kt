package org.rsmod.content.areas.city.draynor

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DraynorVillageLocs @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1(TRAPDOOR_CLOSED) { openTrapdoor(it.loc) }
        onOpLoc1(TRAPDOOR_OPEN) { climbDownTrapdoor(it.loc) }
        onOpLoc1(SEWER_LADDER) { climbUpSewerLadder(it.loc) }
    }

    private suspend fun ProtectedAccess.openTrapdoor(trapdoor: BoundLocInfo) {
        arriveDelay()
        mes("You open the trapdoor.")
        delay(1)
        anim(OPEN_SEQ)
        soundSynth(OPEN_SYNTH)
        delay(1)
        locRepo.change(trapdoor, TRAPDOOR_OPEN, TRAPDOOR_OPEN_DURATION)
    }

    private fun ProtectedAccess.climbDownTrapdoor(trapdoor: BoundLocInfo) {
        telejump(SEWER_LANDINGS[trapdoor.coords] ?: trapdoor.coords.translateZ(DUNGEON_OFFSET))
    }

    private suspend fun ProtectedAccess.climbUpSewerLadder(ladder: BoundLocInfo) {
        anim(CLIMB_SEQ)
        delay(1)
        mes("You climb up the ladder.")
        telejump(ladder.coords.translateZ(-DUNGEON_OFFSET))
    }

    private companion object {
        const val TRAPDOOR_CLOSED = "loc.vampire_trap1"
        const val TRAPDOOR_OPEN = "loc.vampire_trap2"
        const val SEWER_LADDER = "loc.vampire_ladder"
        const val OPEN_SEQ = "seq.human_openchest"
        const val OPEN_SYNTH = "synth.trapdoor_open"
        const val CLIMB_SEQ = "seq.human_reachforladder"
        const val TRAPDOOR_OPEN_DURATION = 500
        const val DUNGEON_OFFSET = 6400

        val SEWER_LANDINGS =
            mapOf(
                CoordGrid(3084, 3272, 0) to CoordGrid(3084, 9671, 0),
                CoordGrid(3118, 3244, 0) to CoordGrid(3118, 9644, 0),
            )
    }
}
