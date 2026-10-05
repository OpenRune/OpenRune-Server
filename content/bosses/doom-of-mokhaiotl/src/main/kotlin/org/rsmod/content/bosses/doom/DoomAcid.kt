package org.rsmod.content.bosses.doom

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlin.math.max
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.bossProjectile
import org.rsmod.api.bosses.runtime.encounter
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.config.refs.done.hitmark_groups
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.player.hit.queueHit
import org.rsmod.api.player.isValidTarget
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid

/**
 * Bleeds per damaging hitsplat. The first always lands just outside the boss's edge in the
 * cardinal direction rolled for that delve level; the rest go the same way, inside the core box or, [scatterPercent] of the
 * time, the wider scatter box.
 */
internal data class AcidSpread(
    val maxBleeds: Int,
    val scatterPercent: Int = 0,
    val scatterAlong: IntRange = DoomAcid.CORE_ALONG,
    val scatterLateral: Int = DoomAcid.CORE_LATERAL,
)

/**
 * Acid blood. It belongs to the run, not the encounter: the pool outlives each boss and carries
 * into the next delve level, and burns whoever stands in it every tick while a boss is alive.
 */
@Singleton
internal class DoomAcid @Inject constructor(private val deps: BossDeps) {
    private val pools: MutableMap<Player, Pool> = IdentityHashMap()

    private val projectile by lazy { PROJECTILE.asRSCM(RSCMType.SPOTANIM) }
    private val splats by lazy { SPLATS.map { SpotanimType(it.asRSCM(RSCMType.SPOTANIM)) } }

    private class Pool(val player: Player) {
        val tiles: MutableSet<CoordGrid> = HashSet()
        var boss: Npc? = null
        var delve: DoomDelve? = null
        var direction: Pair<Int, Int> = CARDINALS.first()
    }

    fun attach(player: Player, boss: Npc, delve: DoomDelve) {
        val existing = pools[player]
        val pool = existing ?: Pool(player).also { pools[player] = it }
        pool.boss = boss
        pool.delve = delve
        pool.direction = CARDINALS[deps.random.of(CARDINALS.size)]
        if (existing == null) burn(pool)
    }

    fun reset(player: Player) {
        pools.remove(player)
    }

    fun onBossHit(boss: Npc, hit: Hit) {
        if (hit.damage <= 0 || boss.hitpoints <= 0) return
        val pool = pools.values.firstOrNull { it.boss === boss } ?: return
        val spread = pool.delve?.acid ?: return
        if (deps.encounter(boss).currentPhaseName != FIGHT_PHASE) return
        spray(pool, boss, spread)
    }

    /** Forgets acid that another loc replaced on the same layer, e.g. the descend hole. */
    fun forget(boss: Npc, tile: CoordGrid) {
        pools.values.firstOrNull { it.boss === boss }?.tiles?.remove(tile)
    }

    fun wipe(player: Player) {
        val pool = pools[player] ?: return
        pool.tiles.toList().forEach { clear(player, it) }
    }

    fun clear(player: Player, tile: CoordGrid) {
        val pool = pools[player] ?: return
        if (!pool.tiles.remove(tile)) return
        deps.locRepo.findExact(tile, LocShape.CentrepieceStraight)
            ?.takeIf { it.id == ACID_LOC.asRSCM(RSCMType.LOC) }
            ?.let { deps.locRepo.del(it, Int.MAX_VALUE) }
    }

    fun restore(player: Player, tile: CoordGrid) {
        val pool = pools[player] ?: return
        if (tile !in pool.tiles || deps.locRepo.findLoc(tile, ROCK_LOC)) return
        deps.locRepo.add(tile, ACID_LOC, Int.MAX_VALUE, LocAngle.North, LocShape.CentrepieceStraight)
    }

    private fun spray(pool: Pool, boss: Npc, spread: AcidSpread) {
        val centre = boss.coords.translate(BOSS_HALF, BOSS_HALF)
        val (dx, dz) = pool.direction
        val tiles =
            List(spread.maxBleeds) { index ->
                val (along, lateral) = if (index == 0) BASE_ALONG to 0 else bleedOffset(spread)
                centre.translate(dx * along - dz * lateral, dz * along + dx * lateral)
            }
        for (tile in tiles) {
            if (!DoomArena.onFloor(boss.spawnCoords, tile)) continue
            val rotation = deps.random.of(SPLATS.size)
            val travel = TRAVEL_BASE + TRAVEL_PER_TILE * max(abs(tile.x - centre.x), abs(tile.z - centre.z))
            deps.bossProjectile(projectile, centre, tile, START_HEIGHT, 0, delay = 0, travel = travel, curve = 0)
            deps.worldRepo.spotanimMap(splats[rotation], tile, delay = travel)
            deps.worldQueues.add(LAND_DELAY) { land(pool, tile, rotation) }
        }
    }

    private fun bleedOffset(spread: AcidSpread): Pair<Int, Int> {
        val scatter = deps.random.of(100) < spread.scatterPercent
        val along = if (scatter) spread.scatterAlong else CORE_ALONG
        val lateral = if (scatter) spread.scatterLateral else CORE_LATERAL
        return deps.random.of(along) to deps.random.of(-lateral..lateral)
    }

    private fun land(pool: Pool, tile: CoordGrid, rotation: Int) {
        if (pools[pool.player] !== pool) return
        if (deps.locRepo.findLoc(tile, ROCK_LOC)) {
            pool.tiles += tile
            return
        }
        if (deps.collision.isWalkBlocked(tile)) return
        pool.tiles += tile
        deps.locRepo.add(tile, ACID_LOC, Int.MAX_VALUE, LocAngle[rotation], LocShape.CentrepieceStraight)
    }

    private fun burn(pool: Pool) {
        deps.repeatTick(
            ticks = Int.MAX_VALUE,
            onTick = { _ ->
                val player = pool.player
                if (pools[player] !== pool || !player.isSlotAssigned) {
                    if (pools[player] === pool) pools.remove(player)
                    return@repeatTick false
                }
                val boss = pool.boss
                if (boss != null && boss.isSlotAssigned && boss.hitpoints > 0 && standsInAcid(pool, player)) {
                    PlayerVenom.tryVenom(player)
                    player.queueHit(
                        boss,
                        1,
                        HitType.Typeless,
                        deps.random.of(BURN_DAMAGE),
                        deps.playerHitModifier,
                        hitmark = hitmark_groups.venom,
                    )
                }
                true
            },
        )
    }

    private fun standsInAcid(pool: Pool, player: Player): Boolean =
        player.isValidTarget() && player.coords in pool.tiles && !deps.locRepo.findLoc(player.coords, ROCK_LOC)

    companion object {
        val CORE_ALONG = 2..6
        const val CORE_LATERAL = 2

        private const val ACID_LOC = "loc.dom_acidpool"
        private const val ROCK_LOC = "loc.dom_rock"
        private const val PROJECTILE = "spotanim.vfx_doom_boss_blood_projectile"

        /** Indexed by the acid loc's rotation. */
        private val SPLATS =
            listOf(
                "spotanim.vfx_doom_boss_blood_splat_n",
                "spotanim.vfx_doom_boss_blood_splat_e",
                "spotanim.vfx_doom_boss_blood_splat_s",
                "spotanim.vfx_doom_boss_blood_splat_w",
            )
        private val CARDINALS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)

        private const val BOSS_HALF = 2
        private const val BASE_ALONG = 3
        private const val START_HEIGHT = 100
        private const val TRAVEL_BASE = 15
        private const val TRAVEL_PER_TILE = 3
        private const val LAND_DELAY = 1
        private val BURN_DAMAGE = 3..7
    }
}
