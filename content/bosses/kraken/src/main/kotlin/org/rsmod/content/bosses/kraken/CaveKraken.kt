package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.death.NpcDeath
import org.rsmod.api.npc.interact.AiPlayerInteractions
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
import org.rsmod.plugin.scripts.ScriptContext

class CaveKraken @Inject constructor(
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
            onHit = {
                whirlpools.touchCombat(npc)
                if (whirlpools.isSubmerged(npc)) {
                    hit.resolvePlayerSource(deps.playerList)?.let { attacker ->
                        whirlpools.wake(npc, attacker, CAVE_KRAKEN, ARISE_ANIM, spec, ::refreshImmunity)
                    }
                }
            },
            onCombatTick = { whirlpools.touchCombat(npc) },
        )

        val krakenType = ServerCacheManager.getNpc(CAVE_KRAKEN.asRSCM(RSCMType.NPC))!!
        onNpcQueue(krakenType, "queue.death") {
            npcDeath.deathWithDrops(this, findHero(deps.playerList)?.coords ?: coords)
        }

        val poolId = POOL.asRSCM(RSCMType.NPC)
        onEvent<NpcStateEvents.Respawn> { if (npc.id == poolId) refreshImmunity(npc) }
        onEvent<NpcStateEvents.Spawn>(poolId.toLong()) { refreshImmunity(npc) }
    }

    private fun refreshImmunity(npc: Npc) {
        whirlpools.setImmunity(npc, melee = true, other = false)
    }

    override val spec =
        boss(CAVE_KRAKEN, POOL) {
            stats(attackRate = ATTACK_RATE)

            val immune = ability("immune") { message(IMMUNE_MESSAGE) }

            val blast =
                ability("blast") {
                    anim(ATTACK_ANIM)
                    projectile {
                        spotanim = "spotanim.waterwave_travel"
                        travel = "projanim.magic_spell"
                        hit {
                            damage(Accuracy(Roll(0..MAGIC_MAX)))
                            type(Magic)
                            missSpotanim(SPLASH)
                            spotanim("spotanim.waterwave_impact", IMPACT_HEIGHT)
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

    private companion object {
        private const val CAVE_KRAKEN = "npc.slayer_kraken"
        private const val POOL = "npc.slayer_kraken_sub"
        private const val ATTACK_RATE = 6
        private const val MAGIC_MAX = 13
    }
}
