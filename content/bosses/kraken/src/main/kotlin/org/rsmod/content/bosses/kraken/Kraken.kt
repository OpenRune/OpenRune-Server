package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.annotations.InternalApi
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.events.NpcHitEvents
import org.rsmod.api.npc.queueDeath
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onNpcQueue
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.ARISE_ANIM
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.ATTACK_ANIM
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.IMMUNE_MESSAGE
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.IMPACT_HEIGHT
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.RANGED_DIVISOR
import org.rsmod.content.bosses.kraken.Whirlpools.Companion.SPLASH
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.npc.NpcStateEvents
import org.rsmod.map.zone.ZoneKey
import org.rsmod.plugin.scripts.ScriptContext

class Kraken @Inject constructor(
    deps: BossDeps,
    aiPlayerInteractions: AiPlayerInteractions,
    private val npcDeath: NpcDeath,
) : BossPluginScript(deps) {

    private val whirlpools = Whirlpools(deps, aiPlayerInteractions)

    override fun ScriptContext.startup() {
        BossCombat.register(
            this,
            spec,
            deps,
            onHit = { disturbOnImpact(this, BOSS, ARISE_ANIM, spec, gated = true) },
            onCombatTick = { whirlpools.touchCombat(npc) },
        )
        BossCombat.register(
            this,
            tentacleSpec,
            deps,
            onHit = {
                disturbOnImpact(this, TENTACLE, TENTACLE_ARISE_ANIM, tentacleSpec, gated = false)
            },
            onCombatTick = { whirlpools.touchCombat(npc) },
        )

        val bossType = ServerCacheManager.getNpc(BOSS.asRSCM(RSCMType.NPC))!!
        onNpcQueue(bossType, "queue.death") {
            val dropCoords = findHero(deps.playerList)?.coords ?: coords
            killTentacles(npc)
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
        val gated = whirlpools.isSubmerged(npc) && (!isBoss || hasUndisturbedTentacles(npc))
        whirlpools.setImmunity(npc, melee = isBoss || gated, other = gated)
    }

    @OptIn(InternalApi::class)
    private fun respawnTentacles(boss: Npc) {
        deps.npcRepo
            .findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS)
            .filter { it.type.id == tentaclePoolId && it.hidden }
            .forEach { it.lifecycleRespawnCycle = deps.mapClock.cycle + 1 }
    }

    private fun killTentacles(boss: Npc) {
        val surfaced =
            deps.npcRepo
                .findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS)
                .filter {
                    it.type.id == tentaclePoolId && it.visType.id != tentaclePoolId && it.hitpoints > 0
                }
                .toList()
        for (tentacle in surfaced) {
            tentacle.hitpoints = 0
            tentacle.queueDeath()
        }
    }

    private fun disturbOnImpact(
        event: NpcHitEvents.Impact,
        into: String,
        anim: String,
        whirlpoolSpec: BossSpec,
        gated: Boolean,
    ) {
        val npc = event.npc
        whirlpools.touchCombat(npc)
        if (!whirlpools.isSubmerged(npc)) return
        val attacker = event.hit.resolvePlayerSource(deps.playerList) ?: return
        if (gated && hasUndisturbedTentacles(npc)) {
            attacker.mes("There was no response.")
            return
        }
        whirlpools.wake(npc, attacker, into, anim, whirlpoolSpec, ::refreshNearby)
    }

    private fun hasUndisturbedTentacles(boss: Npc): Boolean =
        deps.npcRepo.findAll(ZoneKey.from(boss.coords), TENTACLE_ZONE_RADIUS).any {
            it.type.id == tentaclePoolId && it.visType.id == tentaclePoolId
        }

    override val spec =
        boss(BOSS, WHIRLPOOL) {
            stats(attackRate = ATTACK_RATE)

            val immune = ability("immune") { message(IMMUNE_MESSAGE) }

            val blast =
                ability("blast") {
                    anim(ATTACK_ANIM)
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
        private const val TENTACLE_ARISE_ANIM = "seq.tentacle_monster_spawn"

        private const val ATTACK_RATE = 4
        private const val MAGIC_MAX = 28
        private const val TENTACLE_ATTACK_RATE = 4
        private const val TENTACLE_MAX = 2
        private const val TENTACLE_ZONE_RADIUS = 2
        private const val TYPELESS_PENETRATION = 100
    }
}
