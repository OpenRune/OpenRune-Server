package org.rsmod.content.bosses.araxxor

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign
import org.rsmod.api.bosses.runtime.Angles
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.runtime.spawnOwnedLoc
import org.rsmod.api.bosses.runtime.spawnOwnedNpc
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.util.EntityExactMove
import org.rsmod.game.entity.util.PathingEntityCommon
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.Direction
import org.rsmod.game.map.collision.canStep
import org.rsmod.game.map.collision.get
import org.rsmod.game.map.collision.isZoneValid
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.flag.CollisionFlag

private const val BALL_ROLL_ANIM = "seq.araxxor_acid_cannon_projectile"
private const val BALL_BURST_ANIM = "seq.araxxor_acid_cannon_explode"
private const val DRIP_SPOT = "spotanim.araxxor_venom_drip"
private const val ACIDIC_BURST_SPOT = "spotanim.araxyte_acid_spider_explosion"

private const val BALL_STEP_TILES = 2
private const val BALL_SIZE = 3
private const val BALL_MAX_TICKS = 60
private const val BALL_DAMAGE_MIN = 14
private const val BALL_DAMAGE_MAX = 18
private const val BALL_BURST_BLOBS = 6
private const val BALL_BURST_SPREAD = 2
private const val ACIDIC_DEATH_BLOBS = 13
private const val ACIDIC_DEATH_SPREAD = 4

private const val DRIP_TICKS = 7
private const val DRIP_GRAPHIC_SLOT = 2
private const val DRIP_MESSAGE =
    "<col=ff3045>Araxxor covers you in venom. It slowly drips off you and covers the floor."

private const val CLIENT_CYCLES_PER_TICK = 30
private const val BLOB_ATTEMPTS = 40
private const val BLOCKING_FLAGS = CollisionFlag.BLOCK_WALK or CollisionFlag.LOC

private class BlobShape(
    val start: Int,
    val base: Int,
    val perTile: Int,
    val startHeight: Int,
    val endHeight: Int,
    val angle: Int,
    val progress: Int,
)

private val BALL_BURST_SHAPE = BlobShape(30, 90, 10, 254, 0, 35, 0)
private val ACIDIC_DEATH_SHAPE = BlobShape(30, 90, 10, 140, 0, 35, 20)

@Singleton
class AraxxorAcid @Inject constructor(private val deps: BossDeps) {

    fun startBall(boss: Npc, aim: CoordGrid) {
        if (!boss.isSlotAssigned || boss.hitpoints <= 0) return
        val centre = boss.coords.translate(BOSS_CENTRE_OFFSET, BOSS_CENTRE_OFFSET)
        val (dx, dz) = ballDirection(centre, aim)
        val spawn =
            centre
                .translate(dx * BOSS_CENTRE_OFFSET, dz * BOSS_CENTRE_OFFSET)
                .translate(-SMALL_CENTRE_OFFSET, -SMALL_CENTRE_OFFSET)
        val ball = deps.spawnOwnedNpc(boss, BALL_NPC, spawn) ?: return
        ball.respawns = false
        ball.mode = NpcMode.None
        ball.faceSquare(spawn.translate(dx, dz))
        ball.pendingExactMove =
            EntityExactMove(
                deltaX1 = -dx * BALL_STEP_TILES,
                deltaZ1 = -dz * BALL_STEP_TILES,
                clientDelay1 = 0,
                deltaX2 = 0,
                deltaZ2 = 0,
                clientDelay2 = CLIENT_CYCLES_PER_TICK,
                direction = Angles.bearing(dx, dz),
            )
        ball.anim(BALL_ROLL_ANIM, 0, 0)
        val struck = HashSet<Player>()
        sweep(boss, ball.coords, ball.coords.translate(dx * BALL_STEP_TILES, dz * BALL_STEP_TILES), struck)
        roll(boss, ball, dx, dz, struck)
    }

    private fun sweep(boss: Npc, from: CoordGrid, to: CoordGrid, struck: MutableSet<Player>) {
        val minX = minOf(from.x, to.x)
        val maxX = maxOf(from.x, to.x) + BALL_SIZE - 1
        val minZ = minOf(from.z, to.z)
        val maxZ = maxOf(from.z, to.z) + BALL_SIZE - 1
        for (player in deps.playerList) {
            if (!player.isValidTarget() || player.coords.level != from.level) continue
            if (player.coords.x !in minX..maxX || player.coords.z !in minZ..maxZ) continue
            if (!struck.add(player)) continue
            venomHit(boss, player, BALL_DAMAGE_MIN + deps.random.of(BALL_DAMAGE_MAX - BALL_DAMAGE_MIN + 1))
        }
    }

    private fun roll(boss: Npc, ball: Npc, dx: Int, dz: Int, struck: MutableSet<Player>) {
        var bursting = false
        deps.repeatTick(
            ticks = BALL_MAX_TICKS,
            onTick = tick@{
                if (!ball.isSlotAssigned || !boss.isSlotAssigned || boss.hitpoints <= 0) {
                    removeBall(ball)
                    return@tick false
                }
                if (bursting) {
                    ball.anim(BALL_BURST_ANIM, 0, 0)
                    deps.worldQueues.add(1) { removeBall(ball) }
                    return@tick false
                }
                val middle = ball.coords.translate(SMALL_CENTRE_OFFSET, SMALL_CENTRE_OFFSET)
                pool(boss, middle)
                pool(boss, middle.translate(-dx, -dz))

                val stride = openStride(middle, dx, dz)
                if (stride > 0) {
                    sweep(boss, ball.coords, ball.coords.translate(dx * stride, dz * stride), struck)
                    slide(ball, dx, dz, stride)
                }
                if (stride < BALL_STEP_TILES) {
                    bursting = true
                    val burstSource =
                        ball.coords.translate(SMALL_CENTRE_OFFSET - dx, SMALL_CENTRE_OFFSET - dz)
                    throwBlobs(
                        boss,
                        burstSource,
                        scatter(burstSource, BALL_BURST_BLOBS, BALL_BURST_SPREAD, excludeCentre = true),
                        BALL_BURST_SHAPE,
                    )
                }
                true
            },
            onStop = { if (!bursting) removeBall(ball) },
        )
    }

    /** The server teleports the ball and the client draws the glide over the tick. */
    private fun slide(ball: Npc, dx: Int, dz: Int, tiles: Int) {
        val start = ball.coords
        PathingEntityCommon.exactMove(
            ball,
            start,
            start.translate(dx * tiles, dz * tiles),
            delay1 = 0,
            delay2 = CLIENT_CYCLES_PER_TICK,
            dir = Angles.bearing(dx, dz),
            collision = deps.collision,
        )
        ball.anim(BALL_ROLL_ANIM, 0, 0)
    }

    private fun removeBall(ball: Npc) {
        if (ball.isSlotAssigned) deps.npcRepo.del(ball, Int.MAX_VALUE)
    }

    /**
     * How many of the ball's [BALL_STEP_TILES] steps stay on open floor inside allocated zones,
     * counting the tile its 3x3 body will lead with. The last step before a wall is shortened,
     * as captured.
     */
    private fun openStride(middle: CoordGrid, dx: Int, dz: Int): Int {
        val direction = Direction.entries.first { it.xOff == dx && it.zOff == dz }
        var stride = 0
        for (step in 1..BALL_STEP_TILES) {
            val from = middle.translate(dx * (step - 1), dz * (step - 1))
            val front = middle.translate(dx * (step + SMALL_CENTRE_OFFSET), dz * (step + SMALL_CENTRE_OFFSET))
            if (
                !isOpen(from.translate(dx, dz)) ||
                    !deps.collision.isZoneValid(front) ||
                    !deps.collision.canStep(from, direction)
            ) {
                break
            }
            stride = step
        }
        return stride
    }

    private fun isOpen(tile: CoordGrid): Boolean =
        deps.collision.isZoneValid(tile) && deps.collision[tile] and BLOCKING_FLAGS == 0

    fun acidicDeath(boss: Npc, minion: Npc) {
        if (!boss.isSlotAssigned || boss.hitpoints <= 0) return
        minion.spotanim(ACIDIC_BURST_SPOT, 0, 0, 0)
        val middle = minion.coords.translate(SMALL_CENTRE_OFFSET, SMALL_CENTRE_OFFSET)
        throwBlobs(boss, middle, scatter(middle, ACIDIC_DEATH_BLOBS, ACIDIC_DEATH_SPREAD), ACIDIC_DEATH_SHAPE)
    }

    fun startDrip(boss: Npc, player: Player) {
        if (!boss.isSlotAssigned || boss.hitpoints <= 0 || !player.isValidTarget()) return
        player.mes(DRIP_MESSAGE)
        var last: CoordGrid? = null
        var passed: CoordGrid? = null
        var first = true
        deps.repeatTick(
            ticks = DRIP_TICKS,
            onTick = tick@{ remaining ->
                if (!boss.isSlotAssigned || boss.hitpoints <= 0 || !player.isValidTarget()) {
                    return@tick false
                }
                player.spotanim(DRIP_SPOT, 0, 0, DRIP_GRAPHIC_SLOT)
                if (!first) {
                    last?.let { pool(boss, it) }
                    passed?.let { pool(boss, it) }
                }
                val current = player.coords
                passed = runMidpoint(last, current)
                last = current
                first = false
                remaining > 1
            },
        )
    }

    private fun runMidpoint(from: CoordGrid?, to: CoordGrid): CoordGrid? {
        if (from == null) return null
        val dx = to.x - from.x
        val dz = to.z - from.z
        if (max(abs(dx), abs(dz)) != 2) return null
        return from.translate(sign(dx.toFloat()).toInt(), sign(dz.toFloat()).toInt())
    }

    fun pool(boss: Npc, tile: CoordGrid) {
        if (!boss.isSlotAssigned || !isOpen(tile)) return
        deps.spawnOwnedLoc(boss, tile, ACID_POOL_LOC, 0, false)
    }

    private fun venomHit(boss: Npc, player: Player, damage: Int) {
        if (!player.isValidTarget()) return
        player.queueHit(boss, 1, HitType.Typeless, damage, deps.playerHitModifier)
        CombatEffects.venom(player)
    }

    private fun scatter(
        centre: CoordGrid,
        count: Int,
        spread: Int,
        excludeCentre: Boolean = false,
    ): List<CoordGrid> {
        val tiles = LinkedHashSet<CoordGrid>()
        var attempts = 0
        while (tiles.size < count && attempts++ < BLOB_ATTEMPTS) {
            val tile =
                centre.translate(
                    deps.random.of(spread * 2 + 1) - spread,
                    deps.random.of(spread * 2 + 1) - spread,
                )
            if (excludeCentre && tile == centre) continue
            if (isOpen(tile)) tiles += tile
        }
        return tiles.toList()
    }

    private fun throwBlobs(boss: Npc, from: CoordGrid, tiles: List<CoordGrid>, shape: BlobShape) {
        val projectile = POOLS_PROJ.asRSCM(RSCMType.SPOTANIM)
        val splash = SpotanimType(POOLS_SPLASH.asRSCM(RSCMType.SPOTANIM))
        for (tile in tiles) {
            val distance = max(1, max(abs(tile.x - from.x), abs(tile.z - from.z)))
            val end = shape.base + shape.perTile * distance
            deps.bossProjectile(
                projectile,
                from,
                tile,
                shape.startHeight,
                shape.endHeight,
                delay = shape.start,
                travel = end - shape.start,
                curve = shape.angle,
                progress = shape.progress,
            )
            val wholeTicks = end / CLIENT_CYCLES_PER_TICK
            val remainder = end % CLIENT_CYCLES_PER_TICK
            deps.worldQueues.add(max(1, wholeTicks)) {
                deps.worldRepo.spotanimMap(splash, tile, delay = remainder)
                pool(boss, tile)
            }
        }
    }
}
