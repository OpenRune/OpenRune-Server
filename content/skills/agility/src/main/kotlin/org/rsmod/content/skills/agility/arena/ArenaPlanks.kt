package org.rsmod.content.skills.agility.arena

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.agilityLvl
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.table.agility.AgilityArenaRow
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.content.skills.agility.AgilityCourseHook
import org.rsmod.content.skills.agility.AgilityObstacle
import org.rsmod.content.skills.agility.ObstacleLane
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

/**
 * The arena's two sets of three planks
 * (https://oldschool.runescape.wiki/w/Plank_(Brimhaven_Agility_Arena)). Each time the arrow moves,
 * one plank of each set stays whole and the others show broken boards; a broken plank drops anyone
 * below 99 Agility who walks onto it.
 */
@Singleton
class ArenaPlanks
@Inject
constructor(private val random: GameRandom, private val locRepo: LocRepository) {
    private val rows: List<List<CoordGrid>> by lazy {
        val planks = AgilityArenaRow.getRow(ARENA).planks
        planks.groupBy { it.z }.values.map { row -> row.sortedBy { it.x } }
    }

    private val sets: List<List<List<CoordGrid>>> by lazy {
        rows.groupBy { it.first().x }.values.toList()
    }

    private var wholeRows: Set<Int>? = null

    fun reroll() {
        val whole = mutableSetOf<Int>()
        for (set in sets) {
            val kept = random.pick(set)
            whole += kept.first().z
            for (row in set) {
                val broken = if (row === kept) emptySet() else brokenBoards(row.size)
                row.forEachIndexed { index, tile -> lay(tile, broken = index in broken) }
            }
        }
        wholeRows = whole
    }

    fun breaksUnder(access: ProtectedAccess, start: CoordGrid): Boolean {
        if (access.player.agilityLvl >= SAFE_LEVEL) {
            return false
        }
        val whole = wholeRows ?: return false
        val row = rows.firstOrNull { it.leadsFrom(start) } ?: return false
        return row.first().z !in whole
    }

    // UNVERIFIED: how many boards of a bad plank show broken; captures show one to all four.
    private fun brokenBoards(boards: Int): Set<Int> {
        val first = random.of(boards)
        return (0 until boards).filterTo(mutableSetOf(first)) { random.of(2) == 0 }
    }

    private fun lay(tile: CoordGrid, broken: Boolean) {
        val loc = locRepo.findExact(tile, LocShape.GroundDecor) ?: return
        val board = if (broken) BROKEN_BOARD else random.pick(WHOLE_BOARDS)
        if (loc.id != board.asRSCM(RSCMType.LOC)) {
            locRepo.add(tile, board, Int.MAX_VALUE, loc.angle, loc.shape)
        }
    }

    private fun List<CoordGrid>.leadsFrom(start: CoordGrid): Boolean {
        val first = first()
        return start.level == first.level &&
            start.z == first.z &&
            start.x in first.x - START_REACH..last().x + START_REACH
    }

    private companion object {
        const val ARENA = "dbrow.agility_arena"
        const val SAFE_LEVEL = 99
        const val START_REACH = 2
        const val BROKEN_BOARD = "loc.agilityarena_plank_broke1"

        val WHOLE_BOARDS =
            listOf(
                "loc.agilityarena_plank_middle",
                "loc.agilityarena_plank2_middle",
                "loc.agilityarena_plank3_middle",
            )
    }
}

internal class ArenaPlankFalls @Inject constructor(private val planks: ArenaPlanks) :
    AgilityCourseHook {
    private val arenaCourse = AgilityCoursesRow.getRow(ARENA_COURSE).rowId

    override fun falls(access: ProtectedAccess, obstacle: AgilityObstacle, lane: ObstacleLane) =
        !obstacle.isShortcut &&
            obstacle.course.rowId == arenaCourse &&
            planks.breaksUnder(access, lane.start)

    private companion object {
        const val ARENA_COURSE = "dbrow.agility_course_brimhaven_arena"
    }
}
