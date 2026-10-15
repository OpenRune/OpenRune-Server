package org.rsmod.content.areas.city.edgeville.locs

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ObjectServerType
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EdgevilleLocs @Inject constructor(private val locRepo: LocRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpLoc1("loc.trapdoor") { openTrapdoor(it.loc) }
        onOpLoc1("loc.trapdoor_open") { climbDownTrapdoor(it.loc) }
        onOpLoc2("loc.trapdoor_open") { closeTrapdoor(it.loc) }
        onOpLoc1("loc.ladder_from_cellar") { climbUpCellarLadder(it.loc, it.type) }

        onOpLoc1("loc.coffin") { openCoffin(it.loc) }
        onOpLoc1("loc.coffin_open") { closeCoffin(it.loc) }

        for (trapdoor in DAVE_TRAPDOORS) {
            onOpLoc1(trapdoor) { enterDavesCellar() }
        }
        onOpLoc1("loc.100_dave_celler_stairs") { leaveDavesCellar() }
    }

    private suspend fun ProtectedAccess.openTrapdoor(trapdoor: BoundLocInfo) {
        arriveDelay()
        mes("The trapdoor opens...")
        soundSynth("synth.door_open")
        locRepo.change(trapdoor, "loc.trapdoor_open", LOC_DURATION)
    }

    private suspend fun ProtectedAccess.closeTrapdoor(trapdoor: BoundLocInfo) {
        arriveDelay()
        mes("You close the trapdoor.")
        soundSynth("synth.door_close")
        locRepo.change(trapdoor, "loc.trapdoor", LOC_DURATION)
    }

    private suspend fun ProtectedAccess.climbDownTrapdoor(trapdoor: BoundLocInfo) {
        arriveDelay()
        mes("You climb down through the trapdoor...")
        anim("seq.human_pickupfloor")
        delay(1)
        telejump(
            if (trapdoor.coords == TRAPDOOR) DUNGEON_LANDING
            else trapdoor.coords.translateZ(DUNGEON_OFFSET)
        )
    }

    /**
     * Loc-specific handlers win over the ladder's content group, so this also has to cover every
     * other cellar ladder of this type the way DungeonLadderScript does. Only the Edgeville one
     * lands the player beside the trapdoor rather than on it.
     */
    private suspend fun ProtectedAccess.climbUpCellarLadder(
        ladder: BoundLocInfo,
        type: ObjectServerType,
    ) {
        arriveDelay()
        val dest =
            if (ladder.coords == DUNGEON_LADDER) {
                TRAPDOOR_LANDING
            } else {
                player.coords.translateZ(-DUNGEON_OFFSET)
            }
        anim(RSCM.getReverseMapping(RSCMType.SEQ, type.param(params.climb_anim).id))
        delay(1)
        telejump(dest)
    }

    private suspend fun ProtectedAccess.openCoffin(coffin: BoundLocInfo) {
        arriveDelay()
        mes("The coffin creaks open...")
        soundSynth("synth.coffin_open")
        locRepo.change(coffin, "loc.coffin_open", LOC_DURATION)
    }

    private suspend fun ProtectedAccess.closeCoffin(coffin: BoundLocInfo) {
        arriveDelay()
        mes("You close the coffin.")
        soundSynth("synth.coffin_close")
        locRepo.change(coffin, "loc.coffin", LOC_DURATION)
    }

    private suspend fun ProtectedAccess.enterDavesCellar() {
        arriveDelay()
        anim("seq.human_pickupfloor")
        delay(1)
        telejump(DAVES_CELLAR)
    }

    private suspend fun ProtectedAccess.leaveDavesCellar() {
        arriveDelay()
        anim("seq.human_reachforladder_walkmerge", delay = 5)
        delay(1)
        telejump(DAVES_KITCHEN)
    }

    private companion object {
        private const val LOC_DURATION = 500
        private const val DUNGEON_OFFSET = 6400

        private val TRAPDOOR = CoordGrid(3097, 3468, 0)
        private val DUNGEON_LANDING = CoordGrid(3096, 9867, 0)
        private val DUNGEON_LADDER = CoordGrid(3097, 9867, 0)
        private val TRAPDOOR_LANDING = CoordGrid(3096, 3468, 0)

        private val DAVES_CELLAR = CoordGrid(3077, 9893, 0)
        private val DAVES_KITCHEN = CoordGrid(3078, 3493, 0)

        private val DAVE_TRAPDOORS =
            listOf(
                "loc.100_dave_celler_trapdoor",
                "loc.100_dave_celler_trapdoor_closed",
                "loc.100_dave_celler_trapdoor_open",
            )
    }
}
