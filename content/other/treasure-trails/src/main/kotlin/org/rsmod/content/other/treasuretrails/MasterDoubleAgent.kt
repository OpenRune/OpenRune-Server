package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.combat.commons.types.MeleeAttackType

class MasterDoubleAgent @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss("npc.double_agent_master") {
            stats(attackRate = ATTACK_RATE)
            val punch =
                ability("punch") {
                    anim("seq.human_unarmedpunch")
                    hit {
                        damage(Accuracy(Roll(0..MAX_HIT), meleeAttackType = MeleeAttackType.Crush))
                        type(Melee)
                        delay = 1
                    }
                }
            phase("combat") { weightedSelectorRandom { +random(punch, weight = 1) } }
        }

    private companion object {
        const val ATTACK_RATE = 4
        const val MAX_HIT = 19
    }
}
