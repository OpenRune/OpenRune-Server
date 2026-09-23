package org.rsmod.content.bosses.amoxliatl

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcServerType
import dev.openrune.types.aconverted.SpotanimType
import jakarta.inject.Inject
import java.util.IdentityHashMap
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.repeatTick
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.combat.commons.player.finishNpcHit
import org.rsmod.api.npc.heal
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.repo.loc.LocRepository
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onModifyNpcHit
import org.rsmod.api.script.onNpcQueue
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.game.entity.player.PlayerUid
import org.rsmod.game.hit.HitType
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.game.map.collision.isWalkBlocked
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

class Amoxliatl @Inject constructor(deps: BossDeps, private val locRepo: LocRepository) :
    BossPluginScript(deps) {

    private lateinit var iceBlockType: NpcServerType

    private val iceBlockOwner = IdentityHashMap<Npc, Npc>()

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)

        iceBlockType = ServerCacheManager.getNpc(ICE_BLOCK.asRSCM(RSCMType.NPC))!!

        val amoxliatlId = AMOXLIATL_NPC.asRSCM(RSCMType.NPC)
        onEvent<NpcStateEvents.Create> { if (npc.type.id == amoxliatlId) initAmoxliatl(npc) }
        onEvent<NpcStateEvents.Respawn> { if (npc.type.id == amoxliatlId) initAmoxliatl(npc) }

        onModifyNpcHit(iceBlockType) {
            if (!hit.isFromPlayer) return@onModifyNpcHit
            val uid = hit.sourceUid ?: return@onModifyNpcHit
            val attacker = PlayerUid(uid).resolve(deps.playerList) ?: return@onModifyNpcHit
            val maxHit = attacker.vars["varp.com_maxhit"]
            if (maxHit > 0) {
                hit.damage = maxHit
            }
            attacker.actionDelay = attacker.currentMapClock
        }

        onNpcQueue(iceBlockType, "queue.death") {
            noneMode()
            explodeIceBlock(npc, npc.coords, heal = false)
        }

        deps.extensionRegistry.register("amoxliatl.standard_attack_fx") { _, npc, target, _ ->
            val spikeTiles = spawnIceSpikeScatter(target.coords)
            deps.worldQueues.add(ICE_SPIKE_DAMAGE_DELAY) {
                for (player in deps.playerList) {
                    if (player.hitpoints > 0 && player.coords in spikeTiles) {
                        val damage =
                            ICE_SPIKE_DAMAGE_MIN + deps.random.of(ICE_SPIKE_DAMAGE_MAX - ICE_SPIKE_DAMAGE_MIN + 1)
                        player.finishNpcHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
                    }
                }
            }
            val poolCoord = target.coords
            deps.worldQueues.add(POOL_SPAWN_DELAY) { spawnIcyPool(npc, poolCoord) }
        }

        deps.extensionRegistry.register("amoxliatl.track_ice_block") { access, block, _, _ ->
            block.movementLocked = true
            block.lockFacing(block.coords)
            block.anim(UNSTABLE_ICE_SPAWN_SEQ)
            iceBlockOwner[block] = access.npc
            deps.worldQueues.add(UNSTABLE_ICE_TIMEOUT_TICKS) {
                if (block.hitpoints > 0) explodeIceBlock(block, block.coords, heal = true)
            }
        }
    }

    private fun initAmoxliatl(npc: Npc) {
        npc.vars["varn.flat_armour"] = FLAT_ARMOUR
    }

    private fun spawnIceSpikeScatter(center: CoordGrid): Set<CoordGrid> {
        val spot = SpotanimType(ICE_SPIKE_SPOTANIM.asRSCM(RSCMType.SPOTANIM))
        val count = ICE_SPIKE_MIN_COUNT + deps.random.of(ICE_SPIKE_MAX_COUNT - ICE_SPIKE_MIN_COUNT + 1)
        val tiles = mutableSetOf<CoordGrid>()
        repeat(count) {
            val coord = randomWalkableTile(center, ICE_SPIKE_RADIUS) ?: return@repeat
            deps.worldRepo.spotanimMap(spot, coord)
            tiles += coord
        }
        return tiles
    }

    private fun randomWalkableTile(center: CoordGrid, radius: Int): CoordGrid? {
        val candidates = mutableListOf<CoordGrid>()
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val coord = center.translate(dx, dz)
                if (!deps.collision.isWalkBlocked(coord)) candidates += coord
            }
        }
        if (candidates.isEmpty()) return null
        return candidates[deps.random.of(candidates.size)]
    }

    private fun spawnIcyPool(npc: Npc, coord: CoordGrid) {
        if (locRepo.findLoc(coord, ICE_POOL)) return
        val loc = locRepo.add(coord, ICE_POOL, Int.MAX_VALUE, LocAngle.West, LocShape.CentrepieceStraight)
        deps.repeatTick(
            ticks = POOL_DURATION_TICKS,
            onTick = { remaining ->
                if (!locRepo.findLoc(coord, ICE_POOL)) return@repeatTick false
                val occupant = deps.playerList.firstOrNull { it.coords == coord && it.hitpoints > 0 }
                if (occupant != null) {
                    val damage = POOL_DAMAGE_MIN + deps.random.of(POOL_DAMAGE_MAX - POOL_DAMAGE_MIN + 1)
                    occupant.finishNpcHit(npc, 1, HitType.Typeless, damage, deps.playerHitModifier)
                }
                remaining > 1
            },
        )
        deps.worldQueues.add(POOL_DURATION_TICKS) { locRepo.del(loc, Int.MAX_VALUE) }
    }

    private fun explodeIceBlock(block: Npc, coord: CoordGrid, heal: Boolean) {
        val npc = iceBlockOwner.remove(block) ?: return
        block.anim(ICE_DESTROY_SEQ)
        if (heal) {
            npc.heal(SHATTER_HEAL_MIN + deps.random.of(SHATTER_HEAL_MAX - SHATTER_HEAL_MIN + 1), showHitsplat = true)
        }
        deps.worldQueues.add(ICE_DESTROY_ANIM_TICKS) {
            deps.npcRepo.del(block, Int.MAX_VALUE)
            if (heal) {
                for (dx in -1..1) {
                    for (dz in -1..1) {
                        val pool = coord.translate(dx, dz)
                        if (!deps.collision.isWalkBlocked(pool)) spawnIcyPool(npc, pool)
                    }
                }
            } else {
                spawnIcyPool(npc, coord)
            }
        }
    }

    private val icicleCrashEffect: Effect =
        parallel(
            anim("seq.amoxliatl_point"),
            disablePrayers(),
            message(ICICLE_MESSAGE),
            projectile(
                spotanim = ICICLE_PROJECTILE,
                config =
                    ProjectileConfig(
                        startHeight = 448,
                        endHeight = 0,
                        startDelay = 50,
                        angle = 128,
                        travelTime = 100,
                        progress = 83,
                        stepMultiplier = 0,
                    ),
                hit =
                    Effect.Hit(
                        damage = (ICICLE_DAMAGE_MIN..ICICLE_DAMAGE_MAX).roll(),
                        type = Magic,
                        spotanim = ICICLE_IMPACT_SPOTANIM,
                    ),
                resolveOnImpact = true,
            ),
        )

    private val unstableIceEffect: Effect =
        sequence(
            anim("seq.amoxliatl_summon"),
            message("Amoxliatl forms some unstable ice blocks around you."),
            repeat(
                times = 1..4,
                effect =
                    projectile(
                        spotanim = ICICLE_PROJECTILE,
                        target = randomWalkableTile(radius = UNSTABLE_ICE_SPAWN_RADIUS, of = CurrentTarget),
                        impact = ICICLE_IMPACT_SPOTANIM,
                        config =
                            ProjectileConfig(
                                startHeight = 112,
                                endHeight = 0,
                                startDelay = 50,
                                angle = 255,
                                travelTime = 40,
                                progress = 83,
                                stepMultiplier = 0,
                            ),
                        onImpact =
                            summon(
                                npc = ICE_BLOCK,
                                radius = 0,
                                centeredOn = ImpactTile,
                                duration = ICE_BLOCK_FALLBACK_DESPAWN,
                                onSummon = "amoxliatl.track_ice_block",
                            ),
                    ),
            ),
        )

    override val spec =
        boss(AMOXLIATL_NPC) {
            stats(attackRate = 8, aggressionRadius = 8)

            val standardAttack =
                ability("standard_attack") {
                    anim("seq.amoxliatl_attack")
                    hit { damage((0..22).roll()); type(Magic) }
                    include(external("amoxliatl.standard_attack_fx"))
                }

            val special =
                ability("special") {
                    include(
                        choose(
                            selector = Rotation(listOf("icicle_crash", "unstable_ice"), randomStart = true),
                            branches =
                                mapOf(
                                    "icicle_crash" to icicleCrashEffect,
                                    "unstable_ice" to unstableIceEffect,
                                ),
                        ),
                    )
                }

            phase("combat") {
                weightedSelectorRandom { +random(standardAttack, weight = 1) }
                forceEveryAttacks(2, 4, special)
            }
        }

    private companion object {
        private const val AMOXLIATL_NPC = "npc.amoxliatl"

        private const val FLAT_ARMOUR = -2

        private const val ICE_POOL = "loc.amoxliatl_ice"

        private const val ICE_BLOCK = "npc.amoxliatl_ice_block"

        private const val ICICLE_PROJECTILE = "spotanim.vfx_amoxliatl_ice_block_projectile_01"
        private const val ICICLE_IMPACT_SPOTANIM = "spotanim.vfx_amoxliatl_ice_block_projectile_impact_01"
        private const val ICE_SPIKE_SPOTANIM = "spotanim.vfx_amoxliatl_ice_floor_combined_01"
        private const val ICE_SPIKE_MIN_COUNT = 30
        private const val ICE_SPIKE_MAX_COUNT = 60
        private const val ICE_SPIKE_RADIUS = 6
        private const val ICE_SPIKE_DAMAGE_MIN = 6
        private const val ICE_SPIKE_DAMAGE_MAX = 10
        private const val ICE_SPIKE_DAMAGE_DELAY = 3
        private const val UNSTABLE_ICE_SPAWN_RADIUS = 2

        private const val POOL_DAMAGE_MIN = 6
        private const val POOL_DAMAGE_MAX = 10
        private const val POOL_DURATION_TICKS = 30
        private const val POOL_SPAWN_DELAY = 3

        private const val ICICLE_DAMAGE_MIN = 0
        private const val ICICLE_DAMAGE_MAX = 34
        private const val ICICLE_MESSAGE =
            "<col=ff3045>Amoxliatl disables your prayers and launches an icicle attack!</col>"

        private const val UNSTABLE_ICE_SPAWN_SEQ = "seq.amoxliatl_unstable_ice_spawn"

        private const val ICE_DESTROY_SEQ = "seq.vfx_djinn_blue_ice_block_explode_01"
        private const val ICE_DESTROY_ANIM_TICKS = 1
        private const val UNSTABLE_ICE_TIMEOUT_TICKS = 15
        private const val ICE_BLOCK_FALLBACK_DESPAWN = 30
        private const val SHATTER_HEAL_MIN = 16
        private const val SHATTER_HEAL_MAX = 25
    }
}
