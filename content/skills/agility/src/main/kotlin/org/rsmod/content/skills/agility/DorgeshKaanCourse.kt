package org.rsmod.content.skills.agility

import jakarta.inject.Inject
import org.rsmod.api.enums.DorgeshAgilityDelicateEnums.dorgesh_agility_delicate_breaks
import org.rsmod.api.enums.DorgeshAgilityHeavyEnums.dorgesh_agility_heavy_breaks
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.LightSourcesRow
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

/**
 * The Dorgesh-Kaan Agility Course's rules beyond its lanes
 * (https://oldschool.runescape.wiki/w/Dorgesh-Kaan_Agility_Course): a light source to see by, a
 * mithril grapple that one pylon swing in 25 destroys, and parts that break when the player
 * teleports away with them.
 */
internal class DorgeshKaanCourse @Inject constructor() : AgilityCourseHook {
    private val dorgeshKaan = AgilityCoursesRow.getRow(COURSE).rowId

    override fun admits(access: ProtectedAccess, course: AgilityCoursesRow): Boolean {
        if (course.needsLight != true || carriesLight(access)) {
            return true
        }
        // UNVERIFIED: wording guessed.
        access.mes("It's too dark to see where you're going without a light source.")
        return false
    }

    override fun landed(access: ProtectedAccess, obstacle: AgilityObstacle) {
        if (obstacle.requirement != WornRequirement.CrossbowGrapple) {
            return
        }
        if (obstacle.course.rowId != dorgeshKaan) {
            return
        }
        if (access.random.of(GRAPPLE_LOSS_CHANCE) != 0) {
            return
        }
        val grapple = obstacle.requirementObjs.firstOrNull { access.worn.count(it) > 0 } ?: return
        if (access.invDel(access.worn, grapple).success) {
            // UNVERIFIED: wording guessed.
            access.mes("Your grapple breaks as you land.")
        }
    }

    private fun carriesLight(access: ProtectedAccess): Boolean =
        LightSourcesRow.all().any { source ->
            val lit = source.lit.internalName
            access.inv.count(lit) > 0 || access.worn.count(lit) > 0
        }

    companion object {
        private const val COURSE = "dbrow.agility_course_dorgesh_kaan"
        private const val GRAPPLE_LOSS_CHANCE = 25

        private const val CAVES_MIN_X = 2688
        private const val CAVES_MAX_X = 2751
        private const val CAVES_MIN_Z = 5184
        private const val CAVES_MAX_Z = 5247
        private const val TELEPORT_DISTANCE = 16

        private val PARTS by lazy {
            (dorgesh_agility_heavy_breaks.backing + dorgesh_agility_delicate_breaks.backing)
                .mapNotNull { (part, broken) -> broken?.let { part to it } }
        }

        fun teleportedOut(from: CoordGrid, to: CoordGrid): Boolean =
            from.inCaves() && !to.inCaves() && from.chebyshevDistance(to) > TELEPORT_DISTANCE

        fun breakParts(access: ProtectedAccess) {
            for ((part, broken) in PARTS) {
                for (slot in access.inv.indices) {
                    if (access.inv[slot]?.id == part.id) {
                        access.invReplaceSlot(access.inv, slot, count = 1, broken)
                    }
                }
            }
        }

        fun carriesPart(player: Player): Boolean =
            PARTS.any { (part, _) -> player.inv.count(part.internalName) > 0 }

        private fun CoordGrid.inCaves(): Boolean =
            x in CAVES_MIN_X..CAVES_MAX_X && z in CAVES_MIN_Z..CAVES_MAX_Z
    }
}
