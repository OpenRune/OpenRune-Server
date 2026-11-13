package org.rsmod.content.skills.agility.arena

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.game.process.GameLifecycle
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.registry.player.PlayerRegistry
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onPlayerCoordsChanged
import org.rsmod.api.script.onPlayerQueueWithArgs
import org.rsmod.api.table.agility.AgilityArenaTrapsRow
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.proj.ProjAnim
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val FACE_SOUTH = 0
private const val FACE_WEST = 512
private const val FACE_NORTH = 1024
private const val FACE_EAST = 1536

/** Mirrors the `KIND_*` ids of the pack's `AgilityArenaTables`. */
internal enum class TrapKind(val id: Int) {
    SpinningBlades(0),
    FloorSpikes(1),
    PressurePad(2),
    Darts(3),
    SawBlade(4);

    companion object {
        fun of(id: Int): TrapKind = entries.first { it.id == id }
    }
}

internal class ArenaTrap(
    val kind: TrapKind,
    val span: List<CoordGrid>,
    val xpTenths: Int,
    val success: Pair<Int, Int>?,
    val dispensers: List<CoordGrid>,
    val locAngle: Int,
    val loc: CoordGrid?,
) {
    private val alongX = span.first().z == span.last().z

    fun headingFrom(previous: CoordGrid): Int {
        val from = if (alongX) previous.x else previous.z
        val low = if (alongX) span.first().x else span.first().z
        val high = if (alongX) span.last().x else span.last().z
        return when {
            from < low -> 1
            from > high -> -1
            else -> 0
        }
    }

    fun landing(heading: Int): CoordGrid =
        if (heading > 0) span.last().step(heading) else span.first().step(heading)

    fun before(heading: Int): CoordGrid =
        if (heading > 0) span.first().step(-heading) else span.last().step(-heading)

    fun along(tile: CoordGrid, heading: Int, tiles: Int = 1): CoordGrid = tile.step(heading * tiles)

    fun angle(heading: Int): Int =
        when {
            alongX && heading > 0 -> FACE_EAST
            alongX -> FACE_WEST
            heading > 0 -> FACE_NORTH
            else -> FACE_SOUTH
        }

    fun dispenserAhead(heading: Int): CoordGrid? =
        if (heading > 0) dispensers.lastOrNull() else dispensers.firstOrNull()

    fun side(tile: CoordGrid, heading: Int, tiles: Int): CoordGrid =
        if (alongX) tile.translate(0, -heading * tiles) else tile.translate(heading * tiles, 0)

    private fun CoordGrid.step(delta: Int): CoordGrid =
        if (alongX) translate(delta, 0) else translate(0, delta)

    companion object {
        fun loadAll(): List<ArenaTrap> =
            AgilityArenaTrapsRow.all().map { row ->
                ArenaTrap(
                    kind = TrapKind.of(row.kind),
                    span = row.span,
                    xpTenths = row.xp,
                    success = row.success.takeIf { it.size == 2 }?.let { it[0] to it[1] },
                    dispensers = row.dispensers,
                    locAngle = row.locAngle ?: 0,
                    loc = row.loc,
                )
            }
    }
}

internal sealed class TrapEvent {
    class Crossing(val trap: ArenaTrap, val tile: CoordGrid, val heading: Int) : TrapEvent()

    class SawHit(val tile: CoordGrid, val push: CoordGrid, val angle: Int) : TrapEvent()
}

internal fun trapDamage(hitpoints: Int): Int = hitpoints * 5 / 100 + 2

/**
 * The arena's traps. Stepping onto a trap's span sets it off the next tick: the player dives or
 * ducks across it, or on a failed roll is thrown back off it and hurt. The saw blades instead rise
 * on a world clock and throw out whoever stands on them.
 */
class ArenaTrapScript
@Inject
constructor(
    private val locRepo: LocRepository,
    private val worldRepo: WorldRepository,
    private val players: PlayerRegistry,
    private val worldClock: MapClock,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val traps = ArenaTrap.loadAll()
        val crossings =
            traps
                .filter { it.kind != TrapKind.SawBlade }
                .flatMap { trap -> trap.span.map { it to trap } }
                .toMap()
        val blades = traps.filter { it.kind == TrapKind.SawBlade }
        onPlayerCoordsChanged { trigger(player, lastKnownCoords, crossings) }
        onPlayerQueueWithArgs<TrapEvent>(TRAP_QUEUE) { resolve(it.args) }
        onEvent<GameLifecycle.LateCycle> { throwOffBlades(blades) }
        onEvent<GameLifecycle.StartCycle> { raiseBlades(blades) }
    }

    private fun trigger(player: Player, previous: CoordGrid, crossings: Map<CoordGrid, ArenaTrap>) {
        val coords = player.coords
        val trap = crossings[coords] ?: return
        if (previous == coords || previous in trap.span || player.busy) {
            return
        }
        val heading = trap.headingFrom(previous)
        if (heading == 0) {
            return
        }
        player.strongQueue(TRAP_QUEUE, 1, TrapEvent.Crossing(trap, coords, heading))
    }

    private suspend fun ProtectedAccess.resolve(event: TrapEvent) {
        when (event) {
            is TrapEvent.Crossing -> {
                if (coords == event.tile) {
                    busy { cross(event.trap, event.heading) }
                }
            }
            is TrapEvent.SawHit -> sawHit(event)
        }
    }

    private suspend fun ProtectedAccess.busy(block: suspend ProtectedAccess.() -> Unit) {
        player.busy = true
        try {
            block()
        } finally {
            player.busy = false
        }
    }

    private suspend fun ProtectedAccess.cross(trap: ArenaTrap, heading: Int) {
        val passes = passes(trap)
        when (trap.kind) {
            TrapKind.SpinningBlades ->
                if (passes) spinThrough(trap, heading) else spinHit(trap, heading)
            TrapKind.FloorSpikes -> {
                dive(trap, heading)
                delay(1)
                soundSynth("synth.floor_spikes_avoid")
                delay(1)
                payXp(trap)
            }
            TrapKind.PressurePad -> {
                dive(trap, heading)
                soundSynth("synth.collapse_avoid", delay = 20)
                delay(2)
                payXp(trap)
            }
            TrapKind.Darts -> if (passes) duckDarts(trap, heading) else dartHit(trap, heading)
            TrapKind.SawBlade -> Unit
        }
    }

    private fun ProtectedAccess.passes(trap: ArenaTrap): Boolean {
        val success = trap.success ?: return true
        return statRandom(AGILITY, success.first, success.second, 0)
    }

    private fun ProtectedAccess.dive(trap: ArenaTrap, heading: Int) {
        exactMove(coords, trap.landing(heading), 30, 49, trap.angle(heading), TeleportType.Exempt)
        anim(DIVE, delay = 20)
    }

    private suspend fun ProtectedAccess.spinThrough(trap: ArenaTrap, heading: Int) {
        dive(trap, heading)
        soundSynth("synth.blade_swing", delay = 20)
        delay(1)
        twist(trap)
        soundSynth("synth.land_flat", delay = 30)
        delay(2)
        payXp(trap)
    }

    private suspend fun ProtectedAccess.spinHit(trap: ArenaTrap, heading: Int) {
        exactMove(coords, trap.before(heading), 30, 60, trap.angle(heading), TeleportType.Exempt)
        anim(THROWN_BACK, delay = 11)
        delay(1)
        mes("You were hit by the spinning blades!", ChatType.Spam)
        twist(trap)
        soundSynth("synth.blade_hit")
        queueHit(1, HitType.Typeless, trapDamage(player.hitpoints))
    }

    private fun twist(trap: ArenaTrap) {
        val tile = trap.loc ?: return
        val blades = locRepo.findExact(tile, LocShape.CentrepieceStraight) ?: return
        worldRepo.locAnim(blades, BLADES_TWIST)
    }

    private suspend fun ProtectedAccess.duckDarts(trap: ArenaTrap, heading: Int) {
        val tile = coords
        trap.dispenserAhead(heading)?.let { fireDart(it, it.past(trap, heading), target = null) }
        soundSynth("synth.dart_fire_avoid")
        camLookAt(tile, 100, 100, 100)
        camMoveTo(trap.along(tile, heading, 10), 400, 232, 232)
        delay(1)
        anim("seq.agilityarena_dartduck", delay = 70)
        camMoveTo(trap.side(trap.along(tile, heading, -4), heading, 4), 440, 5, 4)
        delay(5)
        teleport(trap.along(tile, heading), TeleportType.Exempt)
        delay(1)
        teleport(trap.landing(heading), TeleportType.Exempt)
        delay(1)
        camReset()
        payXp(trap)
    }

    private suspend fun ProtectedAccess.dartHit(trap: ArenaTrap, heading: Int) {
        trap.dispenserAhead(heading)?.let { fireDart(it, coords, target = player) }
        exactMove(coords, trap.before(heading), 44, 74, trap.angle(heading), TeleportType.Exempt)
        anim(THROWN_BACK, delay = 25)
        soundSynth("synth.dart_fire_hit")
        delay(1)
        queueHit(1, HitType.Typeless, trapDamage(player.hitpoints))
        delay(1)
        mes("You were hit by some darts, something on them makes you feel dizzy!", ChatType.Spam)
        statSub(AGILITY, constant = 2, percent = 0)
    }

    private fun CoordGrid.past(trap: ArenaTrap, heading: Int): CoordGrid =
        trap.along(this, heading, -DART_FLIGHT_TILES)

    private fun fireDart(from: CoordGrid, to: CoordGrid, target: Player?) {
        val dart =
            ProjAnim(
                spotanim = DART.asRSCM(RSCMType.SPOTANIM),
                startHeight = 0,
                endHeight = DART_END_HEIGHT,
                startTime = 0,
                endTime = if (target != null) DART_HIT_CYCLES else DART_MISS_CYCLES,
                angle = DART_ANGLE,
                progress = DART_PROGRESS,
                sourceIndex = 0,
                targetIndex = target?.let { -(it.slotId + 1) } ?: 0,
                startCoord = from,
                endCoord = to,
            )
        worldRepo.projAnim(dart)
    }

    private fun ProtectedAccess.sawHit(event: TrapEvent.SawHit) {
        if (coords != event.tile) {
            return
        }
        exactMove(coords, event.push, 30, 56, event.angle, TeleportType.Exempt)
        anim("seq.human_stumble_back", delay = 30)
        mes("You were hit by the saw blade!", ChatType.Spam)
        soundSynth("synth.saw_rise_and_spin")
        soundSynth("synth.sliced")
        queueHit(1, HitType.Typeless, trapDamage(player.hitpoints))
    }

    private fun ProtectedAccess.payXp(trap: ArenaTrap) {
        if (trap.xpTenths > 0) {
            statAdvance(AGILITY, trap.xpTenths / 10.0)
        }
    }

    private fun raiseBlades(blades: List<ArenaTrap>) {
        if (worldClock.cycle % BLADE_CYCLE_TICKS != 0) {
            return
        }
        for (blade in blades) {
            for (tile in blade.span) {
                val floor = locRepo.findExact(tile, LocShape.GroundDecor) ?: continue
                locRepo.del(floor, BLADE_UP_TICKS)
            }
            locRepo.add(
                blade.span.first(),
                BLADE,
                BLADE_UP_TICKS,
                LocAngle[blade.locAngle],
                LocShape.CentrepieceStraight,
            )
        }
    }

    private fun throwOffBlades(blades: List<ArenaTrap>) {
        if (worldClock.cycle % BLADE_CYCLE_TICKS != 0) {
            return
        }
        for (player in players.findAll(ARENA_ZONES)) {
            if (!player.coords.inArena() || player.busy) {
                continue
            }
            val blade = blades.firstOrNull { player.coords in it.span } ?: continue
            val heading = if (player.coords == blade.span.first()) -1 else 1
            val push = blade.along(player.coords, heading)
            val hit = TrapEvent.SawHit(player.coords, push, blade.angle(-heading))
            player.strongQueue(TRAP_QUEUE, 1, hit)
        }
    }

    private companion object {
        const val TRAP_QUEUE = "queue.agility_arena_trap"
        const val AGILITY = "stat.agility"
        const val DIVE = "seq.agilityarena_dive_player"
        const val THROWN_BACK = "seq.agilityarena_player_spikedback"
        const val BLADE = "loc.agilityarena_trap_timedblade2"
        const val BLADE_CYCLE_TICKS = 7
        const val BLADE_UP_TICKS = 4
        const val BLADES_TWIST = "seq.agilityarena_sawblades_twist"
        const val DART = "spotanim.bullettime_dart"
        const val DART_END_HEIGHT = 10
        const val DART_HIT_CYCLES = 25
        const val DART_MISS_CYCLES = 150
        const val DART_ANGLE = 2
        const val DART_PROGRESS = 40
        const val DART_FLIGHT_TILES = 8

        var Player.busy by boolVarBit("varbit.busy")
    }
}
