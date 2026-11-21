package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.output.spotanimMapPrivate
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.api.table.agility.AgilityTermitesRow
import org.rsmod.game.entity.Player

internal class WyrmTermites @Inject constructor(private val objRepo: ObjRepository) :
    AgilityCourseHook {
    private val markers = AgilityTermitesRow.all()

    override fun landed(access: ProtectedAccess, obstacle: AgilityObstacle) {
        val player = access.player
        val shown = player.termiteMarker
        if (shown == 0) {
            return
        }
        val marker = markers.firstOrNull { it.marker == shown } ?: return
        val course = obstacle.course.rowId
        if (marker.step != obstacle.step || marker.courses.none { it.rowId == course }) {
            return
        }
        player.termiteMarker = 0
        val termites = access.random.of(marker.termites[0], marker.termites[1])
        access.invAddOrDrop(objRepo, TERMITES, termites)
        access.mes("You managed to scoop up <col=ff3045>$termites termites</col>!")
        access.anim(PICKUP)
        access.soundSynth(PICK_SYNTH)
        player.spotanimMapPrivate(DESPAWN.asRSCM(RSCMType.SPOTANIM), marker.tile, height = 0)
        if (access.random.of(100) >= SHARD_PERCENT) {
            return
        }
        val shards = access.random.of(SHARDS)
        access.invAddOrDrop(objRepo, BONE_SHARDS, shards)
        // UNVERIFIED: the colour, placed as on the termite line.
        access.mes("You also find <col=ff3045>$shards piles of bone shards</col> they chipped off!")
    }

    override fun lapDone(access: ProtectedAccess, course: AgilityCoursesRow) {
        val candidates =
            markers.filter { marker -> marker.courses.any { it.rowId == course.rowId } }
        if (candidates.isEmpty() || access.random.of(100) >= SPAWN_PERCENT) {
            return
        }
        access.player.termiteMarker = access.random.pick(candidates).marker
    }

    private companion object {
        const val TERMITES = "obj.varlamore_wyrm_agility_termite"
        const val BONE_SHARDS = "obj.blessed_bone_shard"
        const val PICKUP = "seq.human_pickupfloor"
        const val PICK_SYNTH = "synth.pick"
        const val DESPAWN = "spotanim.wyrm_agility_termite_despawn"

        // UNVERIFIED: the wiki gives no spawn rate; this fits its 5.6 termites an advanced lap.
        const val SPAWN_PERCENT = 35
        const val SHARD_PERCENT = 80
        val SHARDS = 22..38

        var Player.termiteMarker by intVarBit("varbit.varlamore_wyrm_agility_termite_obstacle")
    }
}
