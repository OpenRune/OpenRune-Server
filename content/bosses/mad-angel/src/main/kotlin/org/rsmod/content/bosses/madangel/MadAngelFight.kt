package org.rsmod.content.bosses.madangel

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.HitmarkTypeGroup
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.npc.hit.modifier.NpcHitModifier
import org.rsmod.api.npc.hit.queueHit
import org.rsmod.api.npc.isValidTarget
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.canStep
import org.rsmod.game.map.translate
import org.rsmod.map.CoordGrid

internal var Player.nextHitBuff: Int by intVarBit("varbit.mad_angel_next_hit")

/** The sword hand the angel is swinging from. */
enum class SweepSide {
    Left,
    Right,
}

data class SweepParams(val side: SweepSide, val enraged: Boolean)

/**
 * The Mad Angel's three specials, driven by hand rather than declaratively.
 *
 * The sweep cleaves a quarter of the cathedral floor between the axis the angel faces and its sword
 * hand, dusting each tile as it goes and rewarding a player standing beside the angel with the
 * next-hit buff. The blast throws up to three orbs; a player standing under one bounces it back into
 * the angel for a chunk of its health, a miss detonates the whole floor. The smite strikes on fixed
 * ticks and watches Protect from Magic across the charge: holding it gives the reduced hit, while
 * switching it on in the last tick of the charge (including a 1-tick flick) takes no damage and
 * earns the maximum next-hit buff instead.
 *
 * All three scale their damage by hand where a prayer applies, so their hits carry
 * [MadAngelFight]'s own penetration rather than leaning on the engine's prayer block.
 *
 * Rules from the OSRS Wiki: https://oldschool.runescape.wiki/w/Mad_Angel/Strategies
 *
 * @see [Mad_Angel/Strategies]
 */
@Singleton
class MadAngelFight
@Inject
constructor(
    private val deps: BossDeps,
    private val npcHitModifier: NpcHitModifier,
) {

    fun windup(side: SweepSide, enraged: Boolean): String = windupAnim(side, enraged)

    fun sweepAttackDelay(enraged: Boolean): Int {
        val interval = if (enraged) ENRAGED_CLEAVE_TICKS else CLEAVE_TICKS
        val cleaves = if (enraged) ENRAGED_CLEAVES else CLEAVES
        return cleaves * interval + MELEE_SPEED
    }

    fun smiteAttackDelay(enraged: Boolean): Int {
        val last = if (enraged) ENRAGED_STRIKE_TICKS.max() else STRIKE_TICKS.max()
        return last + 1 + MELEE_SPEED
    }

    fun sweep(npc: Npc, target: Player, params: SweepParams) {
        val enraged = params.enraged
        val interval = if (enraged) ENRAGED_CLEAVE_TICKS else CLEAVE_TICKS
        var remaining = if (enraged) ENRAGED_CLEAVES else CLEAVES
        var side = params.side
        deps.suppressAttacks(npc, sweepAttackDelay(enraged))
        npc.movementLocked = true
        var front = faceTarget(npc, target)

        fun cleave() {
            if (!npc.isValidTarget() || !target.isValidTarget()) {
                npc.movementLocked = false
                npc.clearFacingLock()
                return
            }
            remaining--
            val last = remaining == 0
            val nextSide =
                if (last || deps.random.of(2) == 0) side
                else if (side == SweepSide.Right) SweepSide.Left else SweepSide.Right
            npc.anim(
                when {
                    last -> resetAnim(side)
                    nextSide == side -> stayAnim(side, enraged)
                    else -> swapAnim(side, enraged)
                }
            )

            val centre = centre(npc)
            val cleaved =
                sweepTiles(centre, front, side) { at, step -> deps.collision.canStep(at, step) }
            val dust = SpotanimType(DUST.asRSCM(RSCMType.SPOTANIM))
            for (tile in cleaved) {
                deps.worldRepo.spotanimMap(dust, tile, delay = 2 * centre.chebyshevDistance(tile))
            }

            if (target.coords in cleaved) {
                val roll = deps.random.of(SWEEP_MAX_HIT + 1)
                val damage =
                    if (target.prayingMelee()) roll * SWEEP_PROTECTED_PERCENT / 100 else roll
                target.finishNpcHit(
                    npc,
                    HIT_DELAY,
                    HitType.Typeless,
                    damage,
                    deps.playerHitModifier,
                    penetration = PRAYER_PENETRATION,
                )
            } else if (besideNotBehind(centre, front, side, target.coords)) {
                target.nextHitBuff = NEXT_HIT_ACCURATE
                npc.vars[GUARANTEED_HIT] = 1
            }

            front = faceTarget(npc, target)
            side = nextSide
            if (last) {
                npc.movementLocked = false
                npc.clearFacingLock()
            } else {
                deps.worldQueues.add(interval) { cleave() }
            }
        }

        deps.worldQueues.add(interval) { cleave() }
    }

    fun blast(npc: Npc, target: Player, enraged: Boolean) {
        throwBall(npc, target, enraged, thrown = 1)
    }

    fun smite(npc: Npc, target: Player, enraged: Boolean) {
        deps.suppressAttacks(npc, smiteAttackDelay(enraged))
        npc.movementLocked = true
        val strikes = if (enraged) ENRAGED_STRIKE_TICKS else STRIKE_TICKS
        val last = strikes.max()
        val impact = SpotanimType(SPOT_SMITE_IMPACT.asRSCM(RSCMType.SPOTANIM))
        for (tick in strikes) {
            deps.worldQueues.add(tick) {
                if (npc.isValidTarget() && target.isValidTarget()) {
                    deps.worldRepo.spotanimMap(impact, target.coords)
                }
            }
        }
        var prayingBefore = target.prayingMagic()
        var activatedAt = -1

        fun tick(elapsed: Int) {
            if (!npc.isValidTarget() || !target.isValidTarget()) {
                npc.movementLocked = false
                npc.clearFacingLock()
                return
            }
            val prayingNow = target.prayingMagic()
            if (prayingNow && !prayingBefore) {
                activatedAt = elapsed
            }
            if (elapsed - 1 in strikes) {
                strike(npc, target, prayingNow, prayingBefore, activatedAt, elapsed)
            }
            prayingBefore = prayingNow
            if (elapsed > last) {
                npc.movementLocked = false
                npc.clearFacingLock()
                return
            }
            deps.worldQueues.add(1) { tick(elapsed + 1) }
        }

        deps.worldQueues.add(1) { tick(1) }
    }

    private fun throwBall(npc: Npc, target: Player, enraged: Boolean, thrown: Int) {
        if (!npc.isValidTarget() || !target.isValidTarget()) return
        val flight = if (enraged) ENRAGED_FLIGHT_CYCLES else FLIGHT_CYCLES
        val tile = blastTarget(npc, target)
        deps.bossProjectile(
            spotanim = SPOT_BOMB_PROJECTILE.asRSCM(RSCMType.SPOTANIM),
            src = centre(npc),
            target = tile,
            startHeight = PROJECTILE_START_HEIGHT,
            endHeight = PROJECTILE_END_HEIGHT,
            delay = PROJECTILE_START_CYCLE,
            travel = flight - PROJECTILE_START_CYCLE,
            curve = PROJECTILE_CURVE,
        )
        deps.worldRepo.spotanimMap(
            SpotanimType(SHADOW.asRSCM(RSCMType.SPOTANIM)),
            tile,
            delay = SHADOW_DELAY,
        )
        deps.worldQueues.add(flight / CLIENT_CYCLES_PER_TICK) {
            land(npc, target, enraged, tile, thrown)
        }
    }

    private fun land(npc: Npc, target: Player, enraged: Boolean, tile: CoordGrid, thrown: Int) {
        if (!npc.isValidTarget() || !target.isValidTarget()) return
        if (target.coords != tile) {
            explode(npc, target, tile)
            return
        }
        npc.spotanim(
            BOUNCE,
            delay = BOUNCE_DELAY,
            height = BOUNCE_HEIGHT,
            slot = BOUNCE_SLOT + thrown - 1,
        )
        deps.worldRepo.soundArea(
            tile,
            BOUNCE_SOUND,
            radius = BOMB_SOUND_RADIUS,
            size = BOMB_SOUND_SIZE,
        )
        deps.bossProjectile(
            spotanim = SPOT_BOMB_PROJECTILE.asRSCM(RSCMType.SPOTANIM),
            src = tile,
            target = centre(npc),
            startHeight = PROJECTILE_END_HEIGHT,
            endHeight = PROJECTILE_START_HEIGHT,
            delay = 0,
            travel = CLIENT_CYCLES_PER_TICK,
            curve = BOUNCE_CURVE,
        )
        val damage = deps.random.of(BOUNCE_DAMAGE_MIN, BOUNCE_DAMAGE_MAX)
        deps.worldQueues.add(BOUNCE_DAMAGE_TICKS) {
            if (npc.isValidTarget() && target.isValidTarget()) {
                npc.queueHit(
                    source = target,
                    delay = 1,
                    type = HitType.Typeless,
                    damage = damage,
                    modifier = npcHitModifier,
                    hitmark = BOUNCE_HITMARK,
                )
            }
        }
        if (thrown < BLAST_THROWS) {
            throwBall(npc, target, enraged, thrown + 1)
        }
    }

    private fun explode(npc: Npc, target: Player, tile: CoordGrid) {
        val arena = arenaTiles(npc.spawnCoords)
        val explosion = SpotanimType(EXPLOSION.asRSCM(RSCMType.SPOTANIM))
        for (at in arena) {
            val delay = 2 * (abs(at.x - tile.x) + abs(at.z - tile.z))
            deps.worldRepo.spotanimMap(explosion, at, delay = delay)
        }
        deps.worldRepo.soundArea(
            tile,
            EXPLOSION_SOUND,
            radius = BOMB_SOUND_RADIUS,
            size = BOMB_SOUND_SIZE,
        )
        if (target.isValidTarget() && arena.contains(target.coords)) {
            val roll = deps.random.of(EXPLOSION_MIN_HIT, EXPLOSION_MAX_HIT)
            val damage =
                if (target.prayingMagic()) roll * EXPLOSION_PROTECTED_PERCENT / 100 else roll
            target.finishNpcHit(
                npc,
                HIT_DELAY,
                HitType.Typeless,
                damage,
                deps.playerHitModifier,
                penetration = PRAYER_PENETRATION,
            )
        }
    }

    private fun strike(
        npc: Npc,
        target: Player,
        prayingNow: Boolean,
        prayingBefore: Boolean,
        activatedAt: Int,
        elapsed: Int,
    ) {
        if (perfectSmitePrayer(prayingNow, prayingBefore, activatedAt, elapsed)) {
            target.finishNpcHit(
                npc,
                HIT_DELAY,
                HitType.Typeless,
                0,
                deps.playerHitModifier,
                penetration = PRAYER_PENETRATION,
            )
            target.nextHitBuff = NEXT_HIT_MAX
            npc.vars[GUARANTEED_HIT] = 1
            return
        }
        val roll = deps.random.of(SMITE_MAX_HIT + 1)
        val damage = if (prayingNow) roll * SMITE_PROTECTED_PERCENT / 100 else roll
        target.finishNpcHit(
            npc,
            HIT_DELAY,
            HitType.Typeless,
            damage,
            deps.playerHitModifier,
            penetration = PRAYER_PENETRATION,
        )
    }

    private fun faceTarget(npc: Npc, target: Player): Direction {
        val direction = axisOf(centre(npc), target.coords)
        npc.lockFacingDirection(direction)
        return direction
    }

    /** A floor tile within two tiles of [target], or the tile it stands on, never under the angel. */
    private fun blastTarget(npc: Npc, target: Player): CoordGrid {
        val arena = arenaTiles(npc.spawnCoords)
        val options = mutableListOf<CoordGrid>()
        for (dx in -BLAST_TARGET_RANGE..BLAST_TARGET_RANGE) {
            for (dz in -BLAST_TARGET_RANGE..BLAST_TARGET_RANGE) {
                val tile = target.coords.translate(dx, dz)
                val underBoss =
                    tile.x in npc.coords.x until (npc.coords.x + npc.size) &&
                        tile.z in npc.coords.z until (npc.coords.z + npc.size)
                if (!underBoss && arena.contains(tile)) options += tile
            }
        }
        return if (options.isEmpty()) target.coords else options[deps.random.of(options.size)]
    }

    private fun Player.prayingMelee(): Boolean = vars[PRAYER_MELEE] == 1

    private fun Player.prayingMagic(): Boolean = vars[PRAYER_MAGIC] == 1

    companion object {
        /**
         * A strike is perfectly prayed when the charge loop sees Protect from Magic come on during
         * the damage tick itself. Player input is handled after the charge loop, so that means the
         * player switched it on on the impact tick, one tick before the hit lands. Switching it on
         * any earlier only reduces the hit, while a 1-tick flick that is on for the impact tick is
         * still observed on the damage tick and counts.
         */
        internal fun perfectSmitePrayer(
            prayingNow: Boolean,
            prayingBefore: Boolean,
            activatedAt: Int,
            elapsed: Int,
        ): Boolean =
            activatedAt >= 0 &&
                elapsed - activatedAt == PERFECT_PRAYER_WINDOW &&
                (prayingNow || prayingBefore)

        const val MELEE_SPEED = 6

        const val NEXT_HIT_NONE = 0
        const val NEXT_HIT_ACCURATE = 1
        const val NEXT_HIT_MAX = 2

        internal const val GUARANTEED_HIT = "varn.guaranteed_hit"

        private const val PRAYER_MELEE = "varbit.prayer_protectfrommelee"
        private const val PRAYER_MAGIC = "varbit.prayer_protectfrommagic"

        private const val PRAYER_PENETRATION = 100
        private const val HIT_DELAY = 1
        private const val CLIENT_CYCLES_PER_TICK = 30

        private const val CLEAVES = 4
        private const val CLEAVE_TICKS = 6
        private const val ENRAGED_CLEAVES = 6
        private const val ENRAGED_CLEAVE_TICKS = 4
        private const val SWEEP_MAX_HIT = 31
        private const val SWEEP_PROTECTED_PERCENT = 50
        private const val DUST = "spotanim.zebak_roar_wave_dust"

        private const val BLAST_THROWS = 3
        private const val BLAST_TARGET_RANGE = 2
        private const val FLIGHT_CYCLES = 180
        private const val ENRAGED_FLIGHT_CYCLES = 150
        private const val PROJECTILE_START_CYCLE = 30
        private const val PROJECTILE_CURVE = 40
        private const val BOUNCE_CURVE = 17
        private const val PROJECTILE_START_HEIGHT = 220
        private const val PROJECTILE_END_HEIGHT = 30
        private const val SHADOW_DELAY = 30
        private const val SHADOW = "spotanim.gargboss_debris_shadow_150"
        private const val BOUNCE = "spotanim.vfx_mad_angel_attack_bomb_throw_impact_02"
        private const val BOUNCE_HEIGHT = 30
        private const val BOUNCE_DELAY = 30
        private const val BOUNCE_SLOT = 11
        internal const val SPECIAL_SLOT = 2
        private const val BOUNCE_DAMAGE_MIN = 18
        private const val BOUNCE_DAMAGE_MAX = 22
        private const val BOUNCE_DAMAGE_TICKS = 2
        private const val BOUNCE_SOUND = 12056
        private const val BOMB_SOUND_RADIUS = 20
        private const val BOMB_SOUND_SIZE = 10

        // Reflected bombs wear the plain damage splat even when the roll beats the player's own max hit.
        private val BOUNCE_HITMARK =
            HitmarkTypeGroup(
                lit = hitmark_groups.regular_damage.lit,
                tint = hitmark_groups.regular_damage.tint,
            )
        private const val EXPLOSION = "spotanim.vfx_mad_angel_attack_bomb_throw_impact_01"
        private const val EXPLOSION_MIN_HIT = 30
        private const val EXPLOSION_MAX_HIT = 32
        private const val EXPLOSION_PROTECTED_PERCENT = 50
        private const val EXPLOSION_SOUND = 12054

        private const val SMITE_MAX_HIT = 31
        private const val SMITE_PROTECTED_PERCENT = 25
        private const val PERFECT_PRAYER_WINDOW = 0

        private const val SPOT_BOMB_PROJECTILE =
            "spotanim.vfx_mad_angel_attack_bomb_throw_projectile"
        private const val SPOT_SMITE_IMPACT = "spotanim.vfx_mad_angel_attack_smite_impact"

        private val STRIKE_TICKS = listOf(5)
        private val ENRAGED_STRIKE_TICKS = listOf(5, 9, 11)

        /**
         * Offsets from the angel's spawn tile of the cathedral floor its blast covers, as `z` offset
         * to the inclusive range of `x` offsets on that row.
         */
        private val ARENA_ROWS =
            listOf(
                6 to (-3..3),
                5 to (-3..4),
                4 to (-3..5),
                3 to (-3..5),
                2 to (-3..5),
                1 to (-3..5),
                0 to (-3..5),
                -1 to (-3..5),
                -2 to (-3..5),
                -3 to (-3..5),
                -4 to (-3..4),
                -5 to (-3..3),
            )

        /** How far a cleave wave may travel before it is stopped even without a wall. */
        private const val SWEEP_REACH = 16

        private fun windupAnim(side: SweepSide, enraged: Boolean): String =
            when {
                enraged && side == SweepSide.Right ->
                    "seq.npc_mad_angel_attack_sweep_right_enraged_01"
                enraged -> "seq.npc_mad_angel_attack_sweep_left_enraged_01"
                side == SweepSide.Right -> "seq.npc_mad_angel_attack_sweep_right_01"
                else -> "seq.npc_mad_angel_attack_sweep_left_01"
            }

        private fun stayAnim(side: SweepSide, enraged: Boolean): String =
            when {
                enraged && side == SweepSide.Right ->
                    "seq.npc_mad_angel_attack_sweep_right_enraged_02"
                enraged -> "seq.npc_mad_angel_attack_sweep_left_enraged_02"
                side == SweepSide.Right -> "seq.npc_mad_angel_attack_sweep_right_02"
                else -> "seq.npc_mad_angel_attack_sweep_left_02"
            }

        private fun swapAnim(side: SweepSide, enraged: Boolean): String =
            when {
                enraged && side == SweepSide.Right ->
                    "seq.npc_mad_angel_attack_sweep_right_left_enraged"
                enraged -> "seq.npc_mad_angel_attack_sweep_left_right_enraged"
                side == SweepSide.Right -> "seq.npc_mad_angel_attack_sweep_right_left"
                else -> "seq.npc_mad_angel_attack_sweep_left_right"
            }

        private fun resetAnim(side: SweepSide): String =
            when (side) {
                SweepSide.Right -> "seq.npc_mad_angel_attack_sweep_right_reset"
                SweepSide.Left -> "seq.npc_mad_angel_attack_sweep_left_reset"
            }

        fun arenaTiles(spawn: CoordGrid): List<CoordGrid> =
            ARENA_ROWS.flatMap { (dz, xs) -> xs.map { dx -> spawn.translate(dx, dz) } }

        fun centre(npc: Npc): CoordGrid {
            val half = npc.size / 2
            return npc.coords.translate(half, half)
        }

        /**
         * Every tile a cleave covers: the tiles reachable from [centre] on the side of the bisector
         * between the angel's facing and its sword hand. Flooding outward rather than using a fixed
         * shape lets the wave run on until it meets a wall.
         */
        fun sweepTiles(
            centre: CoordGrid,
            front: Direction,
            side: SweepSide,
            canTravel: (CoordGrid, Direction) -> Boolean,
        ): List<CoordGrid> {
            val (bX, bZ) = bisector(front, side)
            val tiles = ArrayList<CoordGrid>()
            val seen = HashSet<CoordGrid>()
            val pending = ArrayDeque<CoordGrid>()
            seen += centre
            pending += centre
            while (pending.isNotEmpty()) {
                val at = pending.removeFirst()
                tiles += at
                for (step in Direction.CARDINAL) {
                    val next = at.translate(step)
                    val fromX = next.x - centre.x
                    val fromZ = next.z - centre.z
                    if (abs(fromX) > SWEEP_REACH || abs(fromZ) > SWEEP_REACH) continue
                    if (!cleavedHalf(fromX, fromZ, bX, bZ)) continue
                    if (next in seen) continue
                    if (!canTravel(at, step)) continue
                    seen += next
                    pending += next
                }
            }
            return tiles
        }

        private fun bisector(front: Direction, side: SweepSide): Pair<Int, Int> {
            val (frontX, frontZ) = front
            val (handX, handZ) = handOf(front, side)
            return (frontX + handX) to (frontZ + handZ)
        }

        /**
         * True when a tile offset by ([dx], [dz]) lies in the cleaved half-plane whose bisector is
         * ([bX], [bZ]). A tile exactly on the bisector counts only on its counter-clockwise side.
         */
        private fun cleavedHalf(dx: Int, dz: Int, bX: Int, bZ: Int): Boolean {
            val forward = dx * bX + dz * bZ
            if (forward != 0) return forward > 0
            return dx * -bZ + dz * bX >= 0
        }

        /** Out of the cleave on the far side from the sword, level with the angel or ahead. */
        fun besideNotBehind(
            centre: CoordGrid,
            front: Direction,
            side: SweepSide,
            tile: CoordGrid,
        ): Boolean {
            val (frontX, frontZ) = front
            val (handX, handZ) = handOf(front, side)
            val dx = tile.x - centre.x
            val dz = tile.z - centre.z
            val forward = dx * frontX + dz * frontZ
            val lateral = dx * handX + dz * handZ
            return lateral < -1 && forward >= -1
        }

        private fun handOf(front: Direction, side: SweepSide): Direction =
            when {
                side == SweepSide.Right && front == Direction.North -> Direction.East
                side == SweepSide.Right && front == Direction.East -> Direction.South
                side == SweepSide.Right && front == Direction.South -> Direction.West
                side == SweepSide.Right -> Direction.North
                front == Direction.North -> Direction.West
                front == Direction.West -> Direction.South
                front == Direction.South -> Direction.East
                else -> Direction.North
            }

        fun axisOf(from: CoordGrid, to: CoordGrid): Direction {
            val dx = to.x - from.x
            val dz = to.z - from.z
            return if (abs(dx) >= abs(dz)) {
                if (dx >= 0) Direction.East else Direction.West
            } else {
                if (dz >= 0) Direction.North else Direction.South
            }
        }
    }
}
