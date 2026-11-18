package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import org.rsmod.api.table.QuestRow
import org.rsmod.api.table.Tuple7
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.api.table.agility.AgilityObstaclesRow
import org.rsmod.map.CoordGrid

internal class AgilityObstacle(
    private val courseRow: AgilityCoursesRow?,
    private val gamevals: CourseGamevals?,
    val step: Int,
    val locs: List<ObjectServerType>,
    val npcs: List<NpcServerType>,
    val xpTenths: Int,
    val lanes: List<ObstacleLane>,
    val failLanes: Map<Int, ObstacleLane>,
    val success: SuccessCurve?,
    val fallDamage: FallDamage?,
    val diaryXp: DiaryXp?,
    val apRange: Int?,
    val requirement: WornRequirement?,
    val requirementObjs: List<String>,
    val landSpawn: LandSpawn?,
    val handIn: HandIn?,
    val landMesbox: String?,
    val stats: List<ObstacleStat>,
    val startVarbit: StartVarbit?,
    val breaks: Map<ItemServerType, ItemServerType>,
    val shortcut: ShortcutGates?,
    val op: Int,
    val walkOn: Boolean,
) {
    val course: AgilityCoursesRow
        get() = checkNotNull(courseRow) { "A shortcut has no course: $locs" }

    val courseGamevals: CourseGamevals
        get() = checkNotNull(gamevals) { "A shortcut has no course: $locs" }

    val isShortcut: Boolean
        get() = courseRow == null

    val isRecovery: Boolean
        get() = step == RECOVERY_STEP || isShortcut

    val isStepOn: Boolean
        get() = walkOn || locs.isEmpty() && npcs.isEmpty() && lanes.isNotEmpty()

    /**
     * The index of the lane [coords] may start whose start tile is nearest, or `-1`. No landing on
     * its level may be nearer [coords] than that start, so a click from the far side of the
     * obstacle never carries the player back over.
     */
    fun laneFrom(coords: CoordGrid): Int {
        val nearestEnd =
            lanes.filter { it.end.level == coords.level }.minOfOrNull {
                coords.chebyshevDistance(it.end)
            }
        val starts =
            lanes.withIndex().filter { (_, lane) ->
                lane.canStartFrom(coords, apRange) &&
                    (nearestEnd == null || coords.chebyshevDistance(lane.start) <= nearestEnd)
            }
        return starts.minByOrNull { (_, lane) -> coords.chebyshevDistance(lane.start) }?.index ?: -1
    }

    companion object {
        private const val RECOVERY_STEP = -1
        private const val VARBIT_GATE_SIZE = 3

        fun loadAll(): List<AgilityObstacle> {
            val courses = mutableMapOf<Int, CourseGamevals>()
            return AgilityObstaclesRow.all().map { row ->
                val gamevals =
                    row.course?.let { course ->
                        courses.getOrPut(course.rowId) { CourseGamevals(course) }
                    }
                from(row, gamevals)
            }
        }

        private fun from(row: AgilityObstaclesRow, gamevals: CourseGamevals?): AgilityObstacle {
            val messages =
                row.messages.groupBy(
                    keySelector = { it.t0 },
                    valueTransform = { LaneMessage(it.t1, it.t2) },
                )
            val says =
                row.says.groupBy(
                    keySelector = { it.t0 },
                    valueTransform = { LaneMessage(it.t1, it.t2) },
                )
            val actions = actionsByLane(row.actions)
            val lanes =
                row.lanes.map { (index, start) ->
                    ObstacleLane(
                        start,
                        actions.getValue(index),
                        messages[index].orEmpty(),
                        says[index].orEmpty(),
                    )
                }
            val failMessages =
                row.failMessages.groupBy(
                    keySelector = { it.t0 },
                    valueTransform = { LaneMessage(it.t1, it.t2) },
                )
            val failSays =
                row.failSays.groupBy(
                    keySelector = { it.t0 },
                    valueTransform = { LaneMessage(it.t1, it.t2) },
                )
            val failLanes =
                actionsByLane(row.failActions).mapValues { (index, laneActions) ->
                    ObstacleLane(
                        lanes[index].start,
                        laneActions,
                        failMessages[index].orEmpty(),
                        failSays[index].orEmpty(),
                    )
                }
            val success = row.success
            val damage = row.failDamage
            val diary = row.diaryXp
            val handInMessages = row.handInMessages
            val startVarbit = row.startVarbit
            return AgilityObstacle(
                courseRow = row.course,
                gamevals = gamevals,
                step = row.step,
                locs = row.locs,
                npcs = listOfNotNull(row.npcs),
                xpTenths = row.xp,
                lanes = lanes,
                failLanes = failLanes,
                success = if (success.size == 2) SuccessCurve(success[0], success[1]) else null,
                fallDamage =
                    if (damage.size == 4) {
                        FallDamage(damage[0], damage[1], damage[2], damage[3])
                    } else {
                        null
                    },
                diaryXp =
                    if (diary.size == 2) {
                        DiaryXp(RSCM.getReverseMapping(RSCMType.VARBIT, diary[0]), diary[1])
                    } else {
                        null
                    },
                apRange = row.apRange,
                requirement = row.requirement?.let(WornRequirement::of),
                requirementObjs = row.requirementObjs.map { it.internalName },
                landSpawn =
                    row.landSpawnObj?.let { LandSpawn(it.internalName, row.landSpawnTiles) },
                handIn =
                    row.handInObj?.let {
                        HandIn(it.internalName, handInMessages[0], handInMessages[1])
                    },
                landMesbox = row.landMesbox,
                stats =
                    row.stats.map { (stat, level, xp) ->
                        val name = RSCM.getReverseMapping(RSCMType.STAT, stat.id)
                        val displayName = stat.displayName.replaceFirstChar(Char::uppercase)
                        ObstacleStat(name, displayName, level, xp)
                    },
                startVarbit =
                    if (startVarbit.size == 2) {
                        StartVarbit(
                            RSCM.getReverseMapping(RSCMType.VARBIT, startVarbit[0]),
                            startVarbit[1],
                        )
                    } else {
                        null
                    },
                breaks =
                    row.breaks?.backing.orEmpty().mapNotNull { (obj, broken) ->
                        broken?.let { obj to it }
                    }.toMap(),
                shortcut =
                    row.level?.let { level ->
                        ShortcutGates(
                            level = level,
                            quest = row.quest?.let(::RequiredQuest),
                            varbits =
                                row.varbitGates.chunked(VARBIT_GATE_SIZE).map(::varbitGate),
                            refusal = row.refusal,
                            levelRefusal = row.levelRefusal,
                            levelRefusalObj = row.levelRefusalObj?.internalName,
                        )
                    },
                op = row.op ?: 1,
                walkOn = row.walkOn == true,
            )
        }

        private fun varbitGate(values: List<Int>): VarbitGate {
            val (varbit, min, max) = values
            return VarbitGate(RSCM.getReverseMapping(RSCMType.VARBIT, varbit), min..max)
        }

        private fun actionsByLane(
            rows: List<Tuple7<Int, Int, Int, CoordGrid, Int, Int, Int>>
        ): Map<Int, List<ObstacleAction>> =
            rows
                .groupBy(
                    keySelector = { it.t0 },
                    valueTransform = {
                        ObstacleAction(it.t1, ActionOp.of(it.t2), it.t3, it.t4, it.t5, it.t6)
                    },
                )
                .mapValues { (_, it) -> it.sortedBy { action -> action.tick } }
    }
}

internal class CourseGamevals(course: AgilityCoursesRow) {
    val lapCount: String = RSCM.getReverseMapping(RSCMType.VARP, course.lapCount)
    val quest: RequiredQuest? = course.quest?.let(::RequiredQuest)
    val lapStat: LapStat? =
        course.lapStatXp
            .takeIf { it.size == 2 }
            ?.let { (stat, xp) -> LapStat(RSCM.getReverseMapping(RSCMType.STAT, stat), xp) }
}

internal class LapStat(val stat: String, val xpTenths: Int)

internal class RequiredQuest(quest: QuestRow) {
    val key: String = RSCM.getReverseMapping(RSCMType.DBROW, quest.rowId).substringAfter('.')
    val displayName: String = quest.displayname
}

internal class SuccessCurve(val low: Int, val high: Int)

/** A roll in [min]..[max] plus [hpNumerator]/[hpDenominator] of the current hitpoints. */
internal class FallDamage(
    val min: Int,
    val max: Int,
    val hpNumerator: Int,
    val hpDenominator: Int,
)

internal class LandSpawn(val obj: String, val tiles: List<CoordGrid>)

internal class HandIn(val obj: String, val given: String, val refused: String)

/** Mirrors the `REQUIREMENT_*` ids of the pack's `AgilityObstaclesTable`. */
internal enum class WornRequirement(val id: Int) {
    NinjaGreegree(1),
    CrossbowGrapple(2),
    BareHead(3),
    Worn(4),
    NotCarried(5);

    companion object {
        fun of(id: Int): WornRequirement = entries.first { it.id == id }
    }
}

internal class ShortcutGates(
    val level: Int,
    val quest: RequiredQuest?,
    val varbits: List<VarbitGate>,
    val refusal: String?,
    val levelRefusal: String?,
    val levelRefusalObj: String?,
)

internal class VarbitGate(val varbit: String, val values: IntRange)

internal class DiaryXp(val varbit: String, val xpTenths: Int)

internal class ObstacleStat(
    val stat: String,
    val displayName: String,
    val level: Int,
    val xpTenths: Int,
)

internal class StartVarbit(val varbit: String, val value: Int)

internal class LaneMessage(val tick: Int, val text: String)

internal class ObstacleLane(
    val start: CoordGrid,
    val actions: List<ObstacleAction>,
    val messages: List<LaneMessage>,
    val says: List<LaneMessage>,
) {
    val lastTick: Int = maxOf(actions.maxOf { it.tick }, messages.maxOfOrNull { it.tick } ?: 0)

    val end: CoordGrid = actions.lastOrNull { it.op.moves }?.coord ?: start

    val opMessages: List<LaneMessage> = messages.filter { it.tick == OP_TICK }

    val endMesboxes: List<LaneMessage> = messages.filter { it.tick == END_MESBOX_TICK }

    fun canStartFrom(coords: CoordGrid, apRange: Int? = null): Boolean =
        coords.level == start.level &&
            coords.chebyshevDistance(start) <= maxOf(MAX_START_DISTANCE, apRange ?: 0)

    private companion object {
        const val MAX_START_DISTANCE = 4
        const val OP_TICK = -1
        const val END_MESBOX_TICK = -2
    }
}

internal class ObstacleAction(
    val tick: Int,
    val op: ActionOp,
    val coord: CoordGrid,
    val ref: Int,
    val arg1: Int,
    val arg2: Int,
) {
    private val gameval: String? = op.gameval?.let { RSCM.getReverseMapping(it, ref) }

    val symbol: String
        get() = checkNotNull(gameval) { "$op takes no gameval: $ref" }
}

/** Mirrors the `ACTION_*` ids of the pack's `AgilityObstaclesTable`. */
internal enum class ActionOp(
    val id: Int,
    val moves: Boolean = false,
    val gameval: RSCMType? = null,
) {
    Step(0, moves = true),
    Face(1),
    Anim(2, gameval = RSCMType.SEQ),
    ResetAnim(3),
    Sound(4, gameval = RSCMType.SYNTH),
    Balance(5),
    BalanceOff(6),
    ExactMove(7, moves = true),
    Telejump(8, moves = true),
    Land(9),
    Hit(10),
    ExactPath(11),
    FaceAngle(12),
    Wait(13),
    Xp(14),
    Varbit(15, gameval = RSCMType.VARBIT),
    Spotanim(18, gameval = RSCMType.SPOTANIM),
    LocAnim(19, gameval = RSCMType.SEQ),
    NpcSay(22, gameval = RSCMType.NPC),
    Mesbox(26);

    companion object {
        fun of(id: Int): ActionOp = entries.first { it.id == id }
    }
}
