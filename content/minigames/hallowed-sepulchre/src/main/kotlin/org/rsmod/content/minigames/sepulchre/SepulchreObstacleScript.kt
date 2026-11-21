package org.rsmod.content.minigames.sepulchre

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.math.sign
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.script.onApLoc1
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.table.HallowedObstaclesRow
import org.rsmod.game.entity.util.EntityFaceAngle
import org.rsmod.game.type.getOrNull
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal enum class ObstacleKind(val id: Int) {
    Platform(0),
    Drop(1),
    Stairs(2),
    Gate(3),
    Bridge(4),
    Grapple(5),
    Portal(6),
    Barrier(7),
    Stones(8);

    companion object {
        fun of(id: Int): ObstacleKind = entries.first { it.id == id }
    }
}

internal class ObstacleLane(val start: CoordGrid, val end: CoordGrid) {
    val angle: Int
        get() =
            when {
                end.x > start.x -> FACE_EAST
                end.x < start.x -> FACE_WEST
                end.z > start.z -> FACE_NORTH
                else -> FACE_SOUTH
            }

    val middle: CoordGrid
        get() = CoordGrid((start.x + end.x) / 2, (start.z + end.z) / 2, start.level)

    private companion object {
        const val FACE_SOUTH = 0
        const val FACE_WEST = 512
        const val FACE_NORTH = 1024
        const val FACE_EAST = 1536
    }
}

internal class SepulchreObstacle(
    val kind: ObstacleKind,
    val loc: String,
    val xpTenths: Int?,
    val lanes: List<ObstacleLane>,
    val finish: Boolean,
) {
    fun laneFrom(coords: CoordGrid): ObstacleLane? {
        val lane =
            lanes.filter { it.start.level == coords.level }
                .minByOrNull { coords.chebyshevDistance(it.start) } ?: return null
        val behind =
            lane.end.level == coords.level &&
                coords.chebyshevDistance(lane.end) < coords.chebyshevDistance(lane.start)
        return lane.takeUnless { behind }
    }

    fun next(lane: ObstacleLane): ObstacleLane? = lanes.firstOrNull { it.start == lane.end }

    companion object {
        fun loadAll(): List<SepulchreObstacle> =
            HallowedObstaclesRow.all().map { row ->
                SepulchreObstacle(
                    kind = ObstacleKind.of(row.kind),
                    loc = row.loc.internalName,
                    xpTenths = row.xp,
                    lanes = row.lanes.chunked(2).map { (start, end) -> ObstacleLane(start, end) },
                    finish = row.finish == true,
                )
            }
    }
}

/**
 * The obstacles at the end of each path, and the treasure encounters once they are open. A
 * platform, floor 5's stepping stones or the floor-4 gate is a floor's last obstacle: crossing it
 * pauses the timer. A drop or a path's stairs lead to the floor's lower level. A mended bridge, a
 * grapple pillar, a conjured portal or an opened holy barrier is crossed either way and pays the
 * floor's treasure xp. Each plays from its lane's start two ticks after the player arrives, as
 * captured.
 */
class SepulchreObstacleScript : PluginScript() {
    private lateinit var floors: SepulchreFloors

    override fun ScriptContext.startup() {
        floors = SepulchreFloors.load()
        for (obstacle in SepulchreObstacle.loadAll()) {
            onOpLoc1(obstacle.loc) { cross(obstacle) }
            val reach = REACH[obstacle.kind] ?: continue
            onApLoc1(obstacle.loc) {
                if (isWithinApRange(it.loc, reach)) {
                    cross(obstacle)
                }
            }
        }
    }

    private suspend fun ProtectedAccess.cross(obstacle: SepulchreObstacle) {
        val lane = obstacle.laneFrom(coords)
        if (lane == null) {
            mes("You don't see any reason to go back that way.")
            return
        }
        if (obstacle.kind == ObstacleKind.Grapple && !canGrapple()) {
            return
        }
        arriveDelay()
        walkToStart(lane.start)
        player.busy = true
        try {
            if (obstacle.finish) {
                pauseTimer()
            }
            when (obstacle.kind) {
                ObstacleKind.Platform -> jumpPlatform(lane, obstacle)
                ObstacleKind.Drop -> drop(lane, obstacle)
                ObstacleKind.Stairs -> climbDown(lane, obstacle)
                ObstacleKind.Gate -> squeeze(lane, obstacle)
                ObstacleKind.Bridge -> walkBridge(lane, obstacle)
                ObstacleKind.Grapple -> swing(lane, obstacle)
                ObstacleKind.Portal -> passPortal(lane, obstacle)
                ObstacleKind.Barrier -> passBarrier(lane, obstacle)
                ObstacleKind.Stones -> hopStones(lane, obstacle)
            }
        } finally {
            player.busy = false
        }
    }

    private suspend fun ProtectedAccess.walkToStart(start: CoordGrid) {
        while (coords != start) {
            val step = coords.translate((start.x - coords.x).sign, (start.z - coords.z).sign)
            teleport(step, TeleportType.Exempt)
            delay(1)
        }
    }

    private suspend fun ProtectedAccess.jumpPlatform(
        lane: ObstacleLane,
        obstacle: SepulchreObstacle,
    ) {
        mes("You jump across the platform.", ChatType.Spam)
        hop(lane.start, lane.middle, lane.angle)
        delay(2)
        hop(lane.middle, lane.end, lane.angle)
        delay(1)
        resetAnim()
        payXp(obstacle)
    }

    private suspend fun ProtectedAccess.hopStones(
        first: ObstacleLane,
        obstacle: SepulchreObstacle,
    ) {
        var lane: ObstacleLane? = first
        while (lane != null) {
            hop(lane.start, lane.end, lane.angle)
            delay(1)
            lane = obstacle.next(lane)
        }
        payXp(obstacle)
    }

    private fun ProtectedAccess.hop(from: CoordGrid, to: CoordGrid, angle: Int) {
        anim(JUMP_STONES)
        exactMove(from, to, HOP_DELAY1, HOP_DELAY2, angle, TeleportType.Exempt)
        soundSynth(JUMP_SYNTH, delay = JUMP_SYNTH_DELAY)
    }

    private suspend fun ProtectedAccess.drop(lane: ObstacleLane, obstacle: SepulchreObstacle) {
        faceAngle(lane.angle)
        delay(1)
        anim(JUMP_DOWN, delay = JUMP_DOWN_DELAY)
        soundSynth(JUMP_DOWN_SYNTH, delay = JUMP_DOWN_DELAY)
        delay(1)
        telejump(lane.end, TeleportType.Exempt)
        anim(JUMP_DOWN_LAND)
        delay(1)
        resetAnim()
        payXp(obstacle)
    }

    private suspend fun ProtectedAccess.climbDown(lane: ObstacleLane, obstacle: SepulchreObstacle) {
        delay(1)
        val timerRunning = !SepulchreClock.isExpired(player.timeRemaining)
        ifCloseSub(OVERLAY)
        ifOpenFullOverlay(FADE)
        runClientScript(FADE_LEGACY.asRSCM(RSCMType.CLIENTSCRIPT))
        delay(2)
        telejump(lane.end, TeleportType.Exempt)
        faceAngle(lane.angle)
        delay(2)
        fadeOverlay(0, CLEAR, 0, OPAQUE, FADE_CYCLES)
        payXp(obstacle)
        delay(2)
        ifCloseSub(FADE)
        if (timerRunning) {
            ifOpenOverlay(OVERLAY, OVERLAY_TARGET)
        }
    }

    private suspend fun ProtectedAccess.squeeze(lane: ObstacleLane, obstacle: SepulchreObstacle) {
        mes("You squeeze through the gate.", ChatType.Spam)
        anim(SQUEEZE)
        exactMove(
            lane.start,
            lane.end,
            SQUEEZE_DELAY1,
            SQUEEZE_DELAY2,
            lane.angle,
            TeleportType.Exempt,
        )
        delay(2)
        resetAnim()
        payXp(obstacle)
    }

    private suspend fun ProtectedAccess.walkBridge(
        lane: ObstacleLane,
        obstacle: SepulchreObstacle,
    ) {
        anim(LOG_BALANCE, delay = BRIDGE_DELAY1)
        exactMove(
            lane.start,
            lane.end,
            BRIDGE_DELAY1,
            BRIDGE_DELAY2,
            lane.angle,
            TeleportType.Exempt,
        )
        soundSynth(LOG_BALANCE_SYNTH, loops = BRIDGE_SOUND_LOOPS, delay = BRIDGE_DELAY1)
        delay(BRIDGE_TICKS)
        mes("You rapidly make your way across the bridge.", ChatType.Spam)
        resetAnim()
        payXp(obstacle)
        countCrossing(CONSTRUCTION_XP_EARNED)
    }

    private fun ProtectedAccess.canGrapple(): Boolean {
        val weapon = getOrNull(player.righthand)
        val crossbow = weapon != null && weapon.isCategoryType(CROSSBOW)
        val grapple = GRAPPLES.any { worn.count(it) > 0 }
        if (stat(RANGED) < GRAPPLE_RANGED) {
            // UNVERIFIED: the wording.
            mes("You need a Ranged level of $GRAPPLE_RANGED to grapple across.")
            return false
        }
        if (!crossbow || !grapple) {
            // UNVERIFIED: the wording.
            mes("You need a crossbow and a mithril grapple equipped to do that.")
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.swing(lane: ObstacleLane, obstacle: SepulchreObstacle) {
        mes("You fire your grapple at the pillar...", ChatType.Spam)
        delay(1)
        anim(GRAPPLE_SWING, delay = GRAPPLE_DELAY)
        spotanim(GRAPPLE_SPOTANIM, delay = GRAPPLE_DELAY, height = GRAPPLE_HEIGHT)
        soundSynth(GRAPPLE_FIRE_SYNTH)
        soundSynth(GRAPPLE_SYNTH, delay = GRAPPLE_DELAY)
        delay(3)
        exactMove(
            lane.start,
            lane.middle,
            SWING_DELAY1,
            SWING_DELAY2,
            lane.angle,
            TeleportType.Exempt,
        )
        delay(1)
        exactMove(lane.middle, lane.end, 0, SWING_LAND_DELAY, lane.angle, TeleportType.Exempt)
        delay(1)
        mes("...and swing safely to the other side.", ChatType.Spam)
        soundSynth(LAND_SYNTH)
        payXp(obstacle)
        countCrossing(RANGED_XP_EARNED)
    }

    private suspend fun ProtectedAccess.passPortal(
        lane: ObstacleLane,
        obstacle: SepulchreObstacle,
    ) {
        anim(FLOAT_UP, delay = PORTAL_DELAY)
        soundSynth(FLOAT_UP_SYNTH, delay = PORTAL_DELAY)
        delay(1)
        mes("You pass through the portal and end up on the other side.", ChatType.Spam)
        exactMove(
            lane.start,
            lane.end,
            PORTAL_DELAY,
            PORTAL_DELAY2,
            lane.angle,
            TeleportType.Exempt,
        )
        delay(1)
        anim(FLOAT_DOWN)
        soundSynth(FLOAT_DOWN_SYNTH)
        delay(1)
        resetAnim()
        payXp(obstacle)
        countCrossing(MAGIC_XP_EARNED)
    }

    // UNVERIFIED: no capture passes a barrier; the wiki gives five ticks, four with the symbol.
    private suspend fun ProtectedAccess.passBarrier(
        lane: ObstacleLane,
        obstacle: SepulchreObstacle,
    ) {
        val ticks = if (invTotal(inv, HALLOWED_SYMBOL) > 0) BARRIER_TICKS - 1 else BARRIER_TICKS
        exactMove(lane.start, lane.end, 0, ticks * CYCLES_PER_TICK, lane.angle, TeleportType.Exempt)
        delay(ticks)
        payXp(obstacle)
    }

    private fun ProtectedAccess.countCrossing(varbit: String) {
        vars[varbit] = (vars[varbit] + 1).coerceAtMost(MAX_CROSSINGS)
    }

    private fun ProtectedAccess.pauseTimer() {
        player.timerPaused = true
        runClientScript(TIMER_PAUSE.asRSCM(RSCMType.CLIENTSCRIPT), player.timeRemaining)
    }

    private fun ProtectedAccess.faceAngle(angle: Int) {
        player.pendingFaceAngle = EntityFaceAngle(angle)
    }

    private fun ProtectedAccess.payXp(obstacle: SepulchreObstacle) {
        val tenths = obstacle.xpTenths ?: floors.at(coords)?.treasureXpTenths ?: return
        if (tenths > 0) {
            statAdvance(AGILITY, tenths / XP_TENTHS)
        }
    }

    private companion object {
        val REACH =
            mapOf(ObstacleKind.Platform to 2, ObstacleKind.Stones to 2, ObstacleKind.Grapple to 5)
        const val JUMP_STONES = "seq.human_jump_stones"
        const val JUMP_SYNTH = "synth.jump"
        const val JUMP_SYNTH_DELAY = 5
        const val HOP_DELAY1 = 12
        const val HOP_DELAY2 = 27
        const val JUMP_DOWN = "seq.agility_shortcut_wall_jumpdown"
        const val JUMP_DOWN_LAND = "seq.agility_shortcut_wall_jumpdown2"
        const val JUMP_DOWN_SYNTH = "synth.jump2"
        const val JUMP_DOWN_DELAY = 15
        const val SQUEEZE = "seq.railing_squeeze"
        const val SQUEEZE_DELAY1 = 8
        const val SQUEEZE_DELAY2 = 58
        const val FADE = "interface.fade_overlay"
        const val OPAQUE = 255
        const val CLEAR = 0
        const val FADE_CYCLES = 50
        const val XP_TENTHS = 10.0
        const val LOG_BALANCE = "seq.human_walk_logbalance_loop"
        const val LOG_BALANCE_SYNTH = "synth.log_balance"
        const val BRIDGE_DELAY1 = 5
        const val BRIDGE_DELAY2 = 115
        const val BRIDGE_SOUND_LOOPS = 4
        const val BRIDGE_TICKS = 4
        const val GRAPPLE_SWING = "seq.dorgesh_xbow_swing"
        const val GRAPPLE_SPOTANIM = "spotanim.dorgesh_grapple_spot"
        const val GRAPPLE_FIRE_SYNTH = "synth.crossbow_grappling"
        const val GRAPPLE_SYNTH = "synth.dorg_crossbow_grapple"
        const val LAND_SYNTH = "synth.fall_land"
        const val GRAPPLE_DELAY = 6
        const val GRAPPLE_HEIGHT = 82
        const val SWING_DELAY1 = 15
        const val SWING_DELAY2 = 29
        const val SWING_LAND_DELAY = 21
        const val GRAPPLE_RANGED = 62
        const val RANGED = "stat.ranged"
        const val CROSSBOW = "category.crossbow"
        const val FLOAT_UP = "seq.tog_player_float_up"
        const val FLOAT_DOWN = "seq.tog_player_float_down"
        const val FLOAT_UP_SYNTH = "synth.light_creature_up"
        const val FLOAT_DOWN_SYNTH = "synth.light_creature_down"
        const val PORTAL_DELAY = 10
        const val PORTAL_DELAY2 = 60
        const val HALLOWED_SYMBOL = "obj.hallowed_symbol"
        const val BARRIER_TICKS = 5
        const val CYCLES_PER_TICK = 30
        const val MAX_CROSSINGS = 3
        const val CONSTRUCTION_XP_EARNED = "varbit.hallowed_construction_xp_earned"
        const val RANGED_XP_EARNED = "varbit.hallowed_ranged_xp_earned"
        const val MAGIC_XP_EARNED = "varbit.hallowed_magic_xp_earned"

        val GRAPPLES = listOf("obj.xbows_grapple_tip_bolt_mithril_rope", "obj.hallowed_grapple")
    }
}
