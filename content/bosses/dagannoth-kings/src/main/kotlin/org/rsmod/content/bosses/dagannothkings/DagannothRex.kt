package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.Roll
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.soundTo
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.HitType

class DagannothRex @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    override val spec =
        boss(DagannothKingsCommon.REX) {
            stats(attackRate = DagannothKingsCommon.ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim(DagannothKingsCommon.REX_ATTACK_ANIM)
                    soundTo(DagannothKingsCommon.REX_ATTACK_SOUND)
                    faceTarget()
                    hit {
                        damage(
                            Accuracy(
                                Roll(0..DagannothKingsCommon.REX_MAX_HIT),
                                meleeAttackType = MeleeAttackType.Slash,
                            )
                        )
                        type(HitType.Melee)
                        delay = 1
                        onHit(
                            org.rsmod.api.bosses.dsl.soundTo(
                                DagannothKingsCommon.REX_HIT_SOUND,
                            )
                        )
                    }
                }

            phase("combat") {
                weightedSelectorRandom { +random(melee, weight = 1) }
            }
        }
}
