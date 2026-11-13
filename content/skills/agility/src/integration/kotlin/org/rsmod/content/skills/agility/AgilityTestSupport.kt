package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import kotlin.reflect.KClass
import net.rsprot.protocol.game.outgoing.misc.player.MessageGame
import org.rsmod.api.player.interact.ObjInteractions
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.obj.Obj
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript

class AgilityTestDeps
@Inject
constructor(val objRepo: ObjRepository, val objInteractions: ObjInteractions)

internal class Placement(val loc: String, val coords: CoordGrid, val angle: Int, val shape: Int)

private val GNOME_DRAYNOR_PLACEMENTS =
    listOf(
        Placement("loc.gnome_log_balance1", CoordGrid(2474, 3435, 0), 0, 22),
        Placement("loc.obstical_net2", CoordGrid(2471, 3425, 0), 0, 10),
        Placement("loc.obstical_net2", CoordGrid(2473, 3425, 0), 0, 10),
        Placement("loc.obstical_net2", CoordGrid(2475, 3425, 0), 0, 10),
        Placement("loc.climbing_branch", CoordGrid(2473, 3422, 1), 0, 10),
        Placement("loc.balancing_rope", CoordGrid(2478, 3420, 2), 1, 22),
        Placement("loc.climbing_tree", CoordGrid(2486, 3419, 2), 0, 10),
        Placement("loc.climbing_tree2", CoordGrid(2487, 3418, 2), 2, 10),
        Placement("loc.climbing_tree2", CoordGrid(2487, 3420, 2), 0, 10),
        Placement("loc.obstical_net3", CoordGrid(2483, 3426, 0), 2, 10),
        Placement("loc.obstical_net3", CoordGrid(2485, 3426, 0), 2, 10),
        Placement("loc.obstical_net3", CoordGrid(2487, 3426, 0), 2, 10),
        Placement("loc.obstical_pipe3_1", CoordGrid(2484, 3431, 0), 1, 10),
        Placement("loc.obstical_pipe3_1", CoordGrid(2484, 3435, 0), 3, 10),
        Placement("loc.obstical_pipe3_2", CoordGrid(2487, 3431, 0), 1, 10),
        Placement("loc.obstical_pipe3_2", CoordGrid(2487, 3435, 0), 3, 10),
        Placement("loc.rooftops_draynor_wallclimb", CoordGrid(3103, 3279, 0), 0, 5),
        Placement("loc.rooftops_draynor_tightrope_1", CoordGrid(3098, 3277, 3), 3, 22),
        Placement("loc.rooftops_draynor_tightrope_2", CoordGrid(3092, 3276, 3), 2, 22),
        Placement("loc.rooftops_draynor_wallcrossing", CoordGrid(3089, 3264, 3), 0, 10),
        Placement("loc.rooftops_draynor_wallscramble", CoordGrid(3088, 3256, 3), 0, 10),
        Placement("loc.rooftops_draynor_leapdown", CoordGrid(3095, 3255, 3), 0, 10),
        Placement("loc.rooftops_draynor_crate", CoordGrid(3102, 3261, 3), 0, 10),
    )

internal val GNOME_LAP =
    listOf(
        "loc.gnome_log_balance1",
        "loc.obstical_net2",
        "loc.climbing_branch",
        "loc.balancing_rope",
        "loc.climbing_tree",
        "loc.obstical_net3",
        "loc.obstical_pipe3_1",
    )

internal val DRAYNOR_LAP =
    listOf(
        "loc.rooftops_draynor_wallclimb",
        "loc.rooftops_draynor_tightrope_1",
        "loc.rooftops_draynor_tightrope_2",
        "loc.rooftops_draynor_wallcrossing",
        "loc.rooftops_draynor_wallscramble",
        "loc.rooftops_draynor_leapdown",
        "loc.rooftops_draynor_crate",
    )

internal val PLACEMENTS =
    GNOME_DRAYNOR_PLACEMENTS +
        ROOFTOP_PLACEMENTS +
        NORMAL_COURSE_PLACEMENTS +
        PYRAMID_PLACEMENTS +
        DORGESH_KAAN_PLACEMENTS

internal val GNOME_START = CoordGrid(2474, 3436, 0)
internal val DRAYNOR_START = CoordGrid(3104, 3279, 0)

internal const val MARK = "obj.grace"
internal const val BUSY = "varbit.busy"
internal const val MARK_READY = "varp.agility_mark_ready_minute"

/** The harness loads the real collision map but not the map locs, so every obstacle is placed. */
internal fun GameTestState.runAgilityTest(
    vararg scripts: KClass<out PluginScript>,
    body: GameTestScope.(AgilityTestDeps) -> Unit,
): Unit =
    runInjectedGameTest(
        AgilityTestDeps::class,
        AgilityModule(),
        AgilityCourseScript::class,
        *scripts,
    ) {
        deps ->
        advance()
        player.observerUUID = player.uuid
        for (placement in PLACEMENTS) {
            placeMapLoc(
                placement.coords,
                placement.loc,
                LocShape[placement.shape],
                LocAngle[placement.angle],
            )
        }
        body(deps)
    }

internal fun GameTestScope.startAt(coords: CoordGrid) {
    player.teleport(coords)
    advance()
}

internal fun GameTestScope.locNear(loc: String, near: CoordGrid): BoundLocInfo {
    val placement =
        PLACEMENTS.filter { it.loc == loc }
            .minBy {
                val levelPenalty = if (it.coords.level == near.level) 0 else 1000
                it.coords.chebyshevDistance(near) + levelPenalty
            }
    val type = checkNotNull(ServerCacheManager.getObject(loc.asRSCM(RSCMType.LOC)))
    val info =
        findLocs(placement.coords).first { it.coords == placement.coords && it.id == type.id }
    return BoundLocInfo(info, type)
}

internal fun GameTestScope.cross(loc: String, timeoutTicks: Int = 40): List<String> =
    crossNear(loc, player.coords, timeoutTicks)

internal fun GameTestScope.crossNear(
    loc: String,
    near: CoordGrid,
    timeoutTicks: Int = 40,
): List<String> {
    val start = player.coords
    player.opLoc1(locNear(loc, near))
    val messages = mutableListOf<String>()
    var started = false
    repeat(timeoutTicks) {
        advance()
        messages += gameMessages()
        if (player.vars[BUSY] != 0) {
            started = true
        } else if (started) {
            return messages
        }
    }
    error("$loc did not run in $timeoutTicks ticks from $start (at ${player.coords}): $messages")
}

internal fun GameTestScope.crossAll(locs: List<String>): List<String> = locs.flatMap { cross(it) }

internal fun GameTestScope.assertRefused(loc: String, ticks: Int = 8): List<String> {
    val start = player.coords
    val before = agilityXp()
    player.opLoc1(locNear(loc, start))
    val messages = advanceCollecting(ticks)
    assertEquals(start, player.coords)
    assertEquals(before, agilityXp())
    return messages
}

/** `advance` clears the capture client every tick, so this keeps each tick's game messages. */
internal fun GameTestScope.advanceCollecting(ticks: Int): List<String> {
    val all = mutableListOf<String>()
    repeat(ticks) {
        advance()
        all += gameMessages()
    }
    return all
}

internal fun GameTestScope.gameMessages(): List<String> =
    client.filterIsInstance<MessageGame>().map { it.message }

internal fun GameTestScope.agilityXp(): Int = player.statMap.getFineXP("stat.agility")

internal fun fineXp(xp: Double): Int = (xp * 10).toInt()

internal fun GameTestScope.setMarkReady(minute: Int) {
    VarPlayerIntMapSetter.set(player, MARK_READY, minute)
}

internal fun GameTestScope.blockMarks() {
    setMarkReady(currentMinute() + 10)
}

internal fun GameTestScope.markReady(): Int = player.vars[MARK_READY]

internal fun GameTestScope.setAgility(level: Int) {
    player.statMap.setBaseLevel("stat.agility", level.toByte())
    player.statMap.setCurrentLevel("stat.agility", level.toByte())
}

internal fun GameTestScope.setVarbit(varbit: String, value: Int) {
    VarPlayerIntMapSetter.set(player, varbit, value)
}

internal fun currentMinute(): Int = (System.currentTimeMillis() / 60_000L).toInt()

internal fun marksOn(deps: AgilityTestDeps, tiles: List<CoordGrid>): List<Obj> {
    val mark = MARK.asRSCM(RSCMType.OBJ)
    return tiles.flatMap { tile -> deps.objRepo.findAll(tile).filter { it.type == mark }.toList() }
}
