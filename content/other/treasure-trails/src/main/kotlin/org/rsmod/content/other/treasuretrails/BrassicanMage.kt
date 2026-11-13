package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.ProjectileConfig

class BrassicanMage @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss("npc.trail_cabbage_mage") {
            stats(attackRate = ATTACK_RATE)
            val cabbage =
                ability("cabbage") {
                    anim("seq.human_throw")
                    projectile {
                        spotanim = "spotanim.bkfortress_cabbage_travel"
                        config =
                            ProjectileConfig.fixed(
                                startHeight = 40,
                                endHeight = 36,
                                delay = 41,
                                travel = 20,
                                angle = 15,
                                progress = 11,
                            )
                        hit {
                            damage(Accuracy(Roll(0..MAX_HIT), kind = MagicAccuracy))
                            type(Typeless)
                        }
                    }
                }
            phase("combat") { weightedSelectorRandom { +random(cabbage, weight = 1) } }
        }

    private companion object {
        const val ATTACK_RATE = 3
        const val MAX_HIT = 16
    }
}
