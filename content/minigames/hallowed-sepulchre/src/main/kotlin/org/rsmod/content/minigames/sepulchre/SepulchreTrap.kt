package org.rsmod.content.minigames.sepulchre

import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import org.rsmod.api.table.HallowedTrapsRow
import org.rsmod.map.CoordGrid

internal enum class TrapKind(val id: Int) {
    Flames(0),
    Sword(1),
    Bolts(2),
    Lightning(3);

    companion object {
        fun of(id: Int): TrapKind = entries.first { it.id == id }
    }
}

/** A fire loc: one tile wide and three long, lengthwise along x when turned east or west. */
internal class Fire(val coords: CoordGrid, angle: Int) {
    val tiles: List<CoordGrid> =
        List(FIRE_LENGTH) { if (angle % 2 == 0) coords.translateZ(it) else coords.translateX(it) }

    private companion object {
        const val FIRE_LENGTH = 3
    }
}

internal class Trap(
    val floor: Int,
    val kind: TrapKind,
    val statues: List<CoordGrid>,
    val fires: List<Fire>,
    val period: Int,
    val phases: List<Int>,
    val launches: List<CoordGrid>,
    val target: CoordGrid?,
    val speed: Int,
    val hold: Int,
    val volley: Int,
    val npc: NpcServerType?,
    val thrown: ObjectServerType?,
    val attack: Int,
    val strikes: List<CoordGrid>,
) {
    val strikeTiles: Set<CoordGrid> =
        strikes.flatMapTo(LinkedHashSet()) { centre ->
            listOf(
                centre,
                centre.translate(-1, -1),
                centre.translate(1, -1),
                centre.translate(-1, 1),
                centre.translate(1, 1),
            )
        }

    /** How far each launch's lane reaches, the same for every lane of a crossbowman set. */
    val reach: Int
        get() = target?.let { distance(launches.first(), it) } ?: 0

    val heading: Pair<Int, Int>
        get() {
            val end = checkNotNull(target)
            val start = launches.first()
            return (end.x - start.x).sign to (end.z - start.z).sign
        }

    fun laneEnd(lane: Int): CoordGrid = along(launches[lane], reach)

    fun along(from: CoordGrid, tiles: Int): CoordGrid {
        val (dx, dz) = heading
        return from.translate(dx * tiles, dz * tiles)
    }

    companion object {
        fun loadAll(): List<Trap> =
            HallowedTrapsRow.all().map { row ->
                Trap(
                    floor = row.floor,
                    kind = TrapKind.of(row.kind),
                    statues = row.statues,
                    fires = row.fires.map { (coords, angle) -> Fire(coords, angle) },
                    period = row.period,
                    phases = row.phases,
                    launches = row.launches,
                    target = row.target,
                    speed = row.speed ?: 0,
                    hold = row.hold ?: 0,
                    volley = row.volley ?: 0,
                    npc = row.npc,
                    thrown = row.thrown,
                    attack = row.attack ?: 0,
                    strikes = row.strikes,
                )
            }

        fun distance(from: CoordGrid, to: CoordGrid): Int =
            max(abs(to.x - from.x), abs(to.z - from.z))
    }
}

/**
 * The statues' timing, with `t` counted on the floor's clock. Every statue animates on each tick
 * it is in view, as live resends them. A wizard statue starts its attack on the tick of a phase,
 * breathes for `attack` ticks and finishes on the next; its flame burns from the tick after the
 * start to the finish. A thrown sword and a bolt travel `speed` tiles a tick. A priest statue's
 * lightning glows on its tiles the tick before the statue starts, strikes two ticks after the
 * start and fades on the next; whoever stands in it as it strikes, or the tick before, is stunned.
 */
internal object TrapTiming {
    const val FLAME_START = "seq.hallowed_statue_fire_start_attack"
    const val FLAME_ATTACK = "seq.hallowed_statue_fire_attack"
    const val FLAME_FINISH = "seq.hallowed_statue_fire_finish_attack"
    const val FLAME_READY = "seq.hallowed_statue_fire_ready"
    const val FLAME_IDLE = "seq.hallowed_statue_fire_idle"
    const val FIRE_READY = "seq.hallowed_statue_ready_fire_blast"
    const val FIRE_START = "seq.hallowed_statue_start_fire_blast"
    const val FIRE_BLAST = "seq.hallowed_statue_fire_blast"
    const val FIRE_END = "seq.hallowed_statue_end_fire_blast"
    const val FIRE_IDLE = "seq.hallowed_statue_idle_fire_blast"
    const val LIGHTNING_IDLE = "seq.hallowed_statue_lightning_idle"
    const val LIGHTNING_START = "seq.hallowed_statue_lightning_start_attack"
    const val LIGHTNING_ATTACK = "seq.hallowed_statue_lightning_attack"
    const val LIGHTNING_FINISH = "seq.hallowed_statue_lightning_finish_attack"
    const val STRIKE_GLOW = "spotanim.hallowed_statue_lightning_start_strike"
    const val STRIKE = "spotanim.hallowed_statue_lightning_strike"
    const val STRIKE_FADE = "spotanim.hallowed_statue_lightning_end_strike"

    private const val READY_TICKS = 2
    private const val SWORD_LINGER = 2
    private const val LIGHTNING_STRIKE = 2

    fun sinceAttack(t: Int, period: Int, phases: List<Int>): Int =
        phases.minOf { Math.floorMod(t - it, period) }

    fun untilAttack(t: Int, period: Int, phases: List<Int>): Int =
        phases.minOf { Math.floorMod(it - t, period) }

    fun flameStatueSeq(since: Int, until: Int, attack: Int): String =
        when {
            since == 0 -> FLAME_START
            since in 1..attack -> FLAME_ATTACK
            since == attack + 1 -> FLAME_FINISH
            until in 1..READY_TICKS -> FLAME_READY
            else -> FLAME_IDLE
        }

    fun fireSeq(since: Int, attack: Int): String =
        when {
            since == 0 -> FIRE_READY
            since == 1 -> FIRE_START
            since in 2..attack -> FIRE_BLAST
            since == attack + 1 -> FIRE_END
            else -> FIRE_IDLE
        }

    fun flameBurns(since: Int, attack: Int): Boolean = since in 1..attack + 1

    fun lightningStatueSeq(since: Int): String =
        when (since) {
            0 -> LIGHTNING_START
            LIGHTNING_STRIKE -> LIGHTNING_ATTACK
            LIGHTNING_STRIKE + 1 -> LIGHTNING_FINISH
            else -> LIGHTNING_IDLE
        }

    fun lightningGround(since: Int, period: Int): String? =
        when (since) {
            period - 1 -> STRIKE_GLOW
            LIGHTNING_STRIKE -> STRIKE
            LIGHTNING_STRIKE + 1 -> STRIKE_FADE
            else -> null
        }

    fun lightningStuns(since: Int): Boolean =
        since == LIGHTNING_STRIKE || since == LIGHTNING_STRIKE + 1

    fun steps(tiles: Int, speed: Int): Int = (tiles + speed - 1) / speed

    /** The ticks from a sword's throw to its removal, a step past the statue on its way back. */
    fun swordLife(reach: Int, speed: Int, hold: Int): Int =
        steps(reach, speed) + hold + steps(reach + speed, speed) + SWORD_LINGER

    /**
     * How far a sword is from its launch tile [since] ticks after its throw; negative once it is
     * back past the launch, `null` once it is gone.
     */
    fun swordOffset(since: Int, reach: Int, speed: Int, hold: Int): Int? {
        val out = steps(reach, speed)
        val back = steps(reach + speed, speed)
        return when {
            since <= out -> minOf(since * speed, reach)
            since <= out + hold -> reach
            since <= out + hold + back -> reach - minOf((since - out - hold) * speed, reach + speed)
            since < out + hold + back + SWORD_LINGER -> -speed
            else -> null
        }
    }

    fun boltOffset(since: Int, reach: Int, speed: Int): Int? =
        if (since > steps(reach, speed)) null else minOf(since * speed, reach)
}
