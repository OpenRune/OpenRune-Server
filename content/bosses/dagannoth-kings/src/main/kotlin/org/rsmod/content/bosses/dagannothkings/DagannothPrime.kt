package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.Roll
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.TargetExpr

class DagannothPrime @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private fun splash(): Effect =
        hit {
            target = TargetExpr.PlayersIn(DagannothKingsCommon.PRIME_AOE)
            damage(Accuracy(Roll(0..DagannothKingsCommon.PRIME_MAX_HIT)))
            type(HitType.Magic)
            spotanim(DagannothKingsCommon.PRIME_IMPACT, height = 124)
            onHit(
                org.rsmod.api.bosses.dsl.soundTo(
                    DagannothKingsCommon.PRIME_HIT_SOUND,
                )
            )
        }

    private fun cast(): Effect =
        projectile(
            spotanim = DagannothKingsCommon.PRIME_PROJECTILE,
            config = DagannothKingsCommon.PRIME_PROJECTILE_CONFIG,
            onImpact = splash(),
        )

    override val spec =
        boss(DagannothKingsCommon.PRIME) {
            stats(attackRate = DagannothKingsCommon.ATTACK_RATE)

            val magic =
                ability("magic") {
                    anim(DagannothKingsCommon.PRIME_ATTACK_ANIM)
                    sound(DagannothKingsCommon.PRIME_ATTACK_SOUND)
                    faceTarget()
                    include(cast())
                }

            phase("combat") {
                weightedSelectorRandom { +random(magic, weight = 1) }
            }
        }
}
