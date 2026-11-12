package org.rsmod.content.skills.agility

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.agility.AgilityCoursesRow

/**
 * A course's rules beyond its lanes, bound into a set by [AgilityModule]. The obstacle runtime
 * asks every hook, so each one checks that the course or obstacle is its own.
 */
internal interface AgilityCourseHook {
    /** Returns `false` when the player backs out of [obstacle] before it starts. */
    suspend fun mayStart(access: ProtectedAccess, obstacle: AgilityObstacle): Boolean = true

    /** Returns `false`, having told the player why, when they may not take on [course]. */
    fun admits(access: ProtectedAccess, course: AgilityCoursesRow): Boolean = true

    /** Whether [lane] of [obstacle], which has no success curve, plays its fail lane. */
    fun falls(access: ProtectedAccess, obstacle: AgilityObstacle, lane: ObstacleLane): Boolean =
        false

    /** [obstacle] is always on a course; shortcuts land without the hooks. */
    fun landed(access: ProtectedAccess, obstacle: AgilityObstacle) {}

    fun lapDone(access: ProtectedAccess, course: AgilityCoursesRow) {}
}
