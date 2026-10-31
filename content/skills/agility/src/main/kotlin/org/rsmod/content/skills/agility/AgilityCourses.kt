package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.table.agility.AgilityCourseRow
import org.rsmod.api.table.agility.AgilityObstacleRow
import org.rsmod.api.table.agility.AgilityStageRow
import org.rsmod.map.CoordGrid

internal const val STAT_AGILITY: String = "stat.agility"
internal const val MARK_OF_GRACE: String = "obj.grace"
internal const val VARP_COURSE: String = "varp.agility_course"
internal const val VARP_PROGRESS: String = "varp.agility_course_progress"
internal const val VARP_MARK_CLOCK: String = "varp.agility_mark_clock"

/**
 * Where an obstacle puts the player. [x], [z] and [level] are absolute and default to "unchanged";
 * the deltas are applied on top, which is how obstacles that only shift the player a few tiles from
 * wherever they are standing are expressed.
 */
data class Landing(
    val x: Int? = null,
    val z: Int? = null,
    val level: Int? = null,
    val dx: Int = 0,
    val dz: Int = 0,
    val dLevel: Int = 0,
) {
    fun resolve(from: CoordGrid): CoordGrid =
        CoordGrid(
            x = (x ?: from.x) + dx,
            z = (z ?: from.z) + dz,
            level = (level ?: from.level) + dLevel,
        )
}

/**
 * One linked movement of an obstacle. Live builds a crossing out of three of these - climb on,
 * travel, drop off - and the cache names them that way too: `monkeybars_on`, `monkeybars_walk`,
 * `monkeybars_off`.
 *
 * A null [landing] keeps the player still, which is what a mount and a dismount do. [ticks] of 0
 * means "however long the distance takes" at one tile per tick, so a traverse keeps pace with its
 * own looping animation instead of gliding across ahead of the player's feet.
 *
 * [moveTicks] separates the travelling from the animation. A jump is airborne for a moment and then
 * spends the rest of its animation landing, so the movement has to finish early or the player keeps
 * drifting after they have already come down.
 *
 * [perTile] is how many ticks one tile of a traverse takes, and it has to be the length of the
 * animation's own loop or the feet and the ground disagree. Monkeybars loop in one tick and a log
 * balance takes two, which is why a tightrope walked at a monkeybar's pace looks like skating.
 */
data class Stage(
    val anim: String,
    val landing: Landing? = null,
    val ticks: Int = 0,
    val slide: Boolean = true,
    val moveTicks: Int = 0,
    val perTile: Int = 1,
)

/**
 * An obstacle that can be failed. [low] and [high] are the wiki's level-1 and level-99 odds out of
 * 256; the damage is the live formula, a share of the hitpoints the player has left rather than a
 * flat hit, which is why waiting until low health is the way players save food.
 */
data class ObstacleFail(val low: Int, val high: Int, val damageDivisor: Int, val damageBase: Int) {
    fun damage(hitpoints: Int): Int = hitpoints / damageDivisor + damageBase
}

/**
 * [stages] is the real shape of a crossing; when it is empty the obstacle is the older single
 * animate-wait-land, which is all the rooftop courses need.
 */
data class Obstacle(
    val locs: List<String>,
    val landing: Landing,
    val xp: Double,
    val anim: String,
    val ticks: Int = 2,
    val slide: Boolean = false,
    val repeats: Int = 1,
    val fail: ObstacleFail? = null,
    val stages: List<Stage> = emptyList(),
)

/**
 * What a course asks for beyond the Agility level. The greegree and the ring stand in for the
 * quests behind them where live gates on the item rather than on quest progress.
 */
data class CourseReqs(
    val quest: String? = null,
    val worn: List<String> = emptyList(),
    val wornMessage: String = "",
    val grapple: Boolean = false,
) {
    companion object {
        val NONE: CourseReqs = CourseReqs()
    }
}

/**
 * [markOdds] is the chance of a mark of grace on a completed lap once the shared three minute
 * cooldown has passed: two in six for most rooftops, two in five at Rellekka and two in three at
 * Canifis and Ardougne. [markPenalty] is the 80% cut that applies twenty levels above [level] -
 * Canifis is the one course where live never applies it.
 *
 * [petBase] is the course's base for the giant squirrel roll, 1 in `petBase - level * 25`.
 */
data class Course(
    val name: String,
    val level: Int,
    val lapXp: Double,
    val obstacles: List<Obstacle>,
    val markSpawns: List<CoordGrid> = emptyList(),
    val markOdds: Double = 0.0,
    val markPenalty: Boolean = true,
    val petBase: Int = 0,
    val reqs: CourseReqs = CourseReqs.NONE,
) {
    /**
     * One entry per obstacle crossing a lap needs, so an obstacle placed three times in a row -
     * the Barbarian Outpost walls all share one loc - is three steps against one obstacle index.
     */
    val steps: List<Int> = obstacles.flatMapIndexed { index, o -> List(o.repeats) { index } }
}

/** The absolute slot of a landing that is left as it was found. See `AgilityCourseTables`. */
private const val LANDING_UNCHANGED = -1

private fun toLanding(slots: List<Int>): Landing? {
    if (slots.size < 6) {
        return null
    }
    return Landing(
        x = slots[0].takeIf { it != LANDING_UNCHANGED },
        z = slots[1].takeIf { it != LANDING_UNCHANGED },
        level = slots[2].takeIf { it != LANDING_UNCHANGED },
        dx = slots[3],
        dz = slots[4],
        dLevel = slots[5],
    )
}

private fun seqSymbol(id: Int): String = RSCM.getReverseMapping(RSCMType.SEQ, id)

/** The mark odds column keeps the wiki's fraction: numerator, then denominator. */
private fun List<Int>.toOdds(): Double =
    if (size < 2 || this[1] == 0) 0.0 else this[0].toDouble() / this[1]

private fun AgilityStageRow.toStage(): Stage =
    Stage(
        anim = seqSymbol(anim.id),
        landing = toLanding(landing),
        ticks = ticks,
        slide = slide,
        moveTicks = moveTicks,
        perTile = perTile,
    )

private fun AgilityObstacleRow.toObstacle(stages: List<Stage>): Obstacle =
    Obstacle(
        locs = locs.map { RSCM.getReverseMapping(RSCMType.LOC, it.id) },
        landing = toLanding(landing) ?: Landing(),
        xp = xp / 10.0,
        anim = seqSymbol(anim.id),
        ticks = ticks,
        slide = slide,
        repeats = repeats,
        fail = fail.takeIf { it.size >= 4 }?.let { ObstacleFail(it[0], it[1], it[2], it[3]) },
        stages = stages,
    )

private fun AgilityCourseRow.toCourse(obstacles: List<Obstacle>): Course =
    Course(
        name = name,
        level = level,
        lapXp = lapXp / 10.0,
        obstacles = obstacles,
        markSpawns = markSpawns,
        markOdds = markOdds.toOdds(),
        markPenalty = markPenalty,
        petBase = petBase,
        reqs =
            CourseReqs(
                quest = quest,
                worn = worn.map { it.internalName },
                wornMessage = wornMessage.orEmpty(),
                grapple = grapple,
            ),
    )

object AgilityCourses {
    val courses: List<Course> by lazy {
        val stages =
            AgilityStageRow.all()
                .sortedBy(AgilityStageRow::ordinal)
                .groupBy({ it.obstacle.rowId }, AgilityStageRow::toStage)
        val obstacles =
            AgilityObstacleRow.all().sortedBy(AgilityObstacleRow::ordinal).groupBy(
                { it.course.rowId },
                { it.toObstacle(stages[it.rowId].orEmpty()) },
            )
        AgilityCourseRow.all().map { row -> row.toCourse(obstacles[row.rowId].orEmpty()) }
    }
}
