package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript

class BrassicanMage @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss("npc.trail_cabbage_mage") {
            stats(attackRate = ATTACK_RATE)
            val blast =
                ability("blast") {
                    anim("seq.human_castwave")
                    hit {
                        damage(Accuracy(Roll(0..MAX_HIT), kind = MagicAccuracy))
                        type(Typeless)
                        delay = HIT_DELAY
                    }
                }
            phase("combat") { weightedSelectorRandom { +random(blast, weight = 1) } }
        }

    private companion object {
        const val ATTACK_RATE = 3
        const val MAX_HIT = 16
        const val HIT_DELAY = 2
    }
}
