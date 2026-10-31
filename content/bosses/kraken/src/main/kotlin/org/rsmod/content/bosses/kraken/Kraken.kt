package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.runtime.startEncounter
import org.rsmod.api.bosses.runtime.suppressAttacks
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.annotations.InternalApi
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

class Kraken @Inject constructor(
    deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
    private val npcDeath: NpcDeath,
) : BossPluginScript(deps) {

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onHit = { disturbOnImpact(this, BOSS, ARISE_ANIM, gated = true) },
            onCombatTick = { touchCombat(npc) },
        )
        BossCombat.register(
            this,
            tentacleSpec,
            deps,
            onHit = { disturbOnImpact(this, TENTACLE, TENTACLE_ARISE_ANIM, gated = false) },
            onCombatTick = { touchCombat(npc) },
        )

        val bossType = ServerCacheManager.getNpc(BOSS.asRSCM(RSCMType.NPC))!!
        onNpcQueue(bossType, "queue.death") {
            val dropCoords = findHero(deps.playerList)?.coords ?: coords
            despawnTentacles(npc)
            npcDeath.deathWithDrops(this, dropCoords)
        }

        val bossId = bossType.id
        onEvent<NpcStateEvents.Respawn> {
            if (npc.id == bossId) respawnTentacles(npc)
            if (npc.id in poolIds) refreshNearby(npc)
        }
        for (poolId in poolIds) {
            onEvent<NpcStateEvents.Spawn>(poolId.toLong()) { refreshNearby(npc) }
        }
    }

    private val bossPoolId by lazy { WHIRLPOOL.asRSCM(RSCMType.NPC) }
    private val tentaclePoolId by lazy { TENTACLE_WHIRLPOOL.asRSCM(RSCMType.NPC) }
    private val poolIds by lazy { setOf(bossPoolId, tentaclePoolId) }

    private fun refreshNearby(npc: Npc) {
        refreshImmunity(npc)
        deps.npcRepo
            .findAll(ZoneKey.from(npc.coords), TENTACLE_ZONE_RADIUS)
            .filter { it.type.id == bossPoolId && it !== npc }
            .forEach { refreshImmunity(it) }
    }

    private fun refreshImmunity(npc: Npc) {
        val isBoss = npc.type.id == bossPoolId
        val gated = isSubmerged(npc) && (!isBoss || hasUndisturbedTentacles(npc))
        val gatedFlag = if (gated) 1 else 0
        npc.vars[IMMUNE_MELEE] = if (isBoss) 1 else gatedFlag
        npc.vars[IMMUNE_RANGED] = gatedFlag
        npc.vars[IMMUNE_MAGIC] = gatedFlag
    }

    @OptIn(InternalApi::class)
    private fun respawnTentacles(boss: Npc) {
        val whirlpoolId = TENTACLE_WHIRLPOOL.asRSCM(RSCMType.NPC)
        deps.npcRepo
            .findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS)
            .filter { it.type.id == whirlpoolId && it.hidden }
            .forEach { it.lifecycleRespawnCycle = deps.mapClock.cycle + 1 }
    }

    private fun touchCombat(npc: Npc) {
        npc.vars[LAST_COMBAT] = deps.mapClock.cycle
    }

    private fun despawnTentacles(boss: Npc) {
        val whirlpoolId = TENTACLE_WHIRLPOOL.asRSCM(RSCMType.NPC)
        val surfaced =
            deps.npcRepo
                .findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS)
                .filter { it.type.id == whirlpoolId && it.visType.id != whirlpoolId }
                .toList()
        for (tentacle in surfaced) {
            deps.npcRepo.despawn(tentacle, tentacle.type.respawnRate)
        }
    }

    private fun watchIdle(npc: Npc) {
        deps.worldQueues.add(IDLE_CHECK_INTERVAL) {
            if (!npc.isSlotAssigned || isSubmerged(npc)) return@add
            if (deps.mapClock.cycle - npc.vars[LAST_COMBAT] >= IDLE_RESET_TICKS) {
                submerge(npc)
            } else {
                watchIdle(npc)
            }
        }
    }

    @OptIn(InternalApi::class)
    private fun submerge(npc: Npc) {
        val whirlpoolSpec = if (npc.type.id == WHIRLPOOL.asRSCM(RSCMType.NPC)) spec else tentacleSpec
        deps.startEncounter(npc, whirlpoolSpec)
        npc.movementLocked = false
        npc.apRangeOverride = null
        npc.apRequiresLineOfSight = true
        npc.setRespawnValues()
        refreshNearby(npc)
    }

    private fun isSubmerged(npc: Npc): Boolean = npc.visType.id == npc.type.id

    private fun disturbOnImpact(
        event: NpcHitEvents.Impact,
        into: String,
        anim: String,
        gated: Boolean,
    ) {
        val npc = event.npc
        touchCombat(npc)
        if (!isSubmerged(npc)) return
        val attacker = event.hit.resolvePlayerSource(deps.playerList) ?: return
        if (gated && hasUndisturbedTentacles(npc)) {
            attacker.mes("There was no response.")
            return
        }
        wake(npc, attacker, into, anim)
    }

    private fun hasUndisturbedTentacles(boss: Npc): Boolean {
        val whirlpoolId = TENTACLE_WHIRLPOOL.asRSCM(RSCMType.NPC)
        return deps.npcRepo.findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS).any {
            it.type.id == whirlpoolId && it.visType.id == whirlpoolId
        }
    }

    @OptIn(InternalApi::class)
    private fun wake(pool: Npc, target: Player, into: String, ariseAnim: String) {
        val surfaced = ServerCacheManager.getNpc(into.asRSCM(RSCMType.NPC)) ?: return
        val damageTaken = pool.type.hitpoints - pool.hitpoints
        pool.transmog(surfaced, Int.MAX_VALUE)
        pool.copyStats(surfaced)
        pool.hitpoints = max(surfaced.hitpoints - damageTaken, 1)
        refreshNearby(pool)
        pool.anim(ariseAnim, 0, 1)
        pool.apRangeOverride = ATTACK_RANGE
        pool.apRequiresLineOfSight = false
        pool.movementLocked = true
        pool.mode = NpcMode.ApPlayer2
        pool.apPlayer2(target, aiPlayerInteractions)
        deps.suppressAttacks(pool, WAKE_ATTACK_DELAY)
        touchCombat(pool)
        watchIdle(pool)
    }

    override val spec =
        boss(BOSS, WHIRLPOOL) {
            stats(attackRate = ATTACK_RATE)

            val immune = ability("immune") { message("You can't attack it properly like that.") }

            val blast =
                ability("blast") {
                    anim(BOSS_ATTACK_ANIM)
                    projectile {
                        spotanim = "spotanim.firewave_travel"
                        travel = "projanim.magic_spell"
                        hit {
                            damage(Accuracy(Roll(0..MAGIC_MAX), kind = MagicalRanged))
                            type(Magic)
                            penetration(TYPELESS_PENETRATION)
                            missSpotanim(SPLASH)
                            spotanim("spotanim.firewave_impact", IMPACT_HEIGHT)
                        }
                    }
                }

            incoming {
                rule(Condition.HitStyle(Melee)) {
                    cap(0)
                    run(immune)
                }
                rule(Condition.HitStyle(Ranged)) { divideMinOne(RANGED_DIVISOR) }
            }

            phase("combat") {
                attackRate = ATTACK_RATE
                weightedSelectorRandom { +random(blast, weight = 1) }
            }
        }

    private val tentacleSpec =
        boss(TENTACLE, TENTACLE_WHIRLPOOL) {
            stats(attackRate = TENTACLE_ATTACK_RATE)

            val blast =
                ability("blast") {
                    anim("seq.tentacle_monster_attack")
                    projectile {
                        spotanim = "spotanim.waterwave_travel"
                        travel = "projanim.magic_spell"
                        hit {
                            damage(Accuracy(Roll(0..TENTACLE_MAX), kind = MagicalRanged))
                            type(Magic)
                            penetration(TYPELESS_PENETRATION)
                            missSpotanim(SPLASH)
                            spotanim("spotanim.waterwave_impact", IMPACT_HEIGHT)
                        }
                    }
                }

            phase("combat") { weightedSelectorRandom { +random(blast, weight = 1) } }
        }

    private companion object {
        private const val BOSS = "npc.slayer_kraken_boss"
        private const val WHIRLPOOL = "npc.slayer_kraken_boss_whirlpool"
        private const val TENTACLE = "npc.slayer_kraken_boss_tentacle"
        private const val TENTACLE_WHIRLPOOL = "npc.slayer_kraken_boss_tentacle_whirlpool"
        private const val ARISE_ANIM = "seq.slayer_kraken_arise"
        private const val TENTACLE_ARISE_ANIM = "seq.tentacle_monster_spawn"

        private const val BOSS_ATTACK_ANIM = "seq.swan_queen_spellcast"
        private const val SPLASH = "spotanim.failedspell_impact"
        private const val IMPACT_HEIGHT = 124
        private const val LAST_COMBAT = "varn.lastcombat"
        private const val IMMUNE_MELEE = "varn.immune_melee"
        private const val IMMUNE_RANGED = "varn.immune_ranged"
        private const val IMMUNE_MAGIC = "varn.immune_magic"
        private const val IDLE_RESET_TICKS = 50
        private const val IDLE_CHECK_INTERVAL = 5
        private const val WAKE_ATTACK_DELAY = 3
        private const val ATTACK_RANGE = 10
        private const val ATTACK_RATE = 4
        private const val MAGIC_MAX = 28
        private const val TENTACLE_ATTACK_RATE = 4
        private const val TENTACLE_MAX = 2
        private const val TENTACLE_ZONE_RADIUS = 2
        private const val TYPELESS_PENETRATION = 100
        private const val RANGED_DIVISOR = 7
    }
}
