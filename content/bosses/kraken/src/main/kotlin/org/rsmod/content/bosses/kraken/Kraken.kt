package org.rsmod.content.bosses.kraken

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.NpcMode
import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossCombat
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Condition
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.npc.apPlayer2
import org.rsmod.api.npc.interact.AiPlayerInteractions
import org.rsmod.api.script.onOpNpc2
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.ScriptContext

class Kraken @Inject constructor(
    deps: BossDeps,
    private val aiPlayerInteractions: AiPlayerInteractions,
) : BossPluginScript(deps) {

    override fun ScriptContext.startup() {
        BossCombat.register(this, spec, deps)
        BossCombat.register(this, tentacleSpec, deps)

        onOpNpc2(WHIRLPOOL) { wake(it.npc, player, BOSS) }
        onOpNpc2(TENTACLE_WHIRLPOOL) { wake(it.npc, player, TENTACLE) }
    }

    private fun wake(pool: Npc, target: Player, into: String) {
        val surfaced = ServerCacheManager.getNpc(into.asRSCM(RSCMType.NPC)) ?: return
        pool.transmog(surfaced, Int.MAX_VALUE)
        pool.anim(ARISE_ANIM, 0, 1)
        pool.mode = NpcMode.ApPlayer2
        pool.apPlayer2(target, aiPlayerInteractions)
    }

    override val spec =
        boss(BOSS, WHIRLPOOL) {
            stats(attackRate = ATTACK_RATE)

            val immune = ability("immune") { message("The Kraken resists your attack.") }

            val blast =
                ability("blast") {
                    anim("seq.human_caststrike")
                    projectile(
                        spotanim = "spotanim.waterwave_travel",
                        impact = "spotanim.waterwave_impact",
                        hit = Effect.Hit(damage = (0..MAGIC_MAX).roll(), type = Magic),
                    )
                }

            incoming {
                rule(Condition.HitStyle(Melee)) {
                    cap(0)
                    run(immune)
                }
                rule(Condition.HitStyle(Ranged)) {
                    cap(0)
                    run(immune)
                }
            }

            phase("combat") {
                attackRate = ATTACK_RATE
                weightedSelectorRandom { +random(blast, weight = 1) }
            }
        }

    private val tentacleSpec =
        boss(TENTACLE, TENTACLE_WHIRLPOOL) {
            stats(attackRate = TENTACLE_ATTACK_RATE)

            val slap =
                ability("slap") {
                    anim("seq.tentacle_monster_attack")
                    hit {
                        damage((0..TENTACLE_MAX).roll())
                        type(Melee)
                    }
                }

            phase("combat") {
                weightedSelectorRandom { +random(slap, weight = 1, requires = WithinMeleeRange) }
            }
        }

    private companion object {
        private const val BOSS = "npc.slayer_kraken_boss"
        private const val WHIRLPOOL = "npc.slayer_kraken_boss_whirlpool"
        private const val TENTACLE = "npc.slayer_kraken_boss_tentacle"
        private const val TENTACLE_WHIRLPOOL = "npc.slayer_kraken_boss_tentacle_whirlpool"
        private const val ARISE_ANIM = "seq.slayer_kraken_arise"

        private const val ATTACK_RATE = 4
        private const val MAGIC_MAX = 28
        private const val TENTACLE_ATTACK_RATE = 4
        private const val TENTACLE_MAX = 13
    }
}
