package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.Roll
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.onEach
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.TargetExpr

class DagannothSupreme @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private fun shot(): Effect =
        projectile(
            spotanim = DagannothKingsCommon.SUPREME_PROJECTILE,
            config = DagannothKingsCommon.SUPREME_PROJECTILE_CONFIG,
            hit =
                hit {
                    damage(Accuracy(Roll(0..DagannothKingsCommon.SUPREME_MAX_HIT)))
                    type(HitType.Ranged)
                    onHit(
                        org.rsmod.api.bosses.dsl.soundTo(
                            DagannothKingsCommon.SUPREME_HIT_SOUND,
                        )
                    )
                },
        )

    private fun volley(): Effect =
        onEach(
            TargetExpr.PlayersIn(DagannothKingsCommon.SUPREME_AREA),
            shot(),
        )

    override val spec =
        boss(DagannothKingsCommon.SUPREME) {
            stats(attackRate = DagannothKingsCommon.ATTACK_RATE)

            val ranged =
                ability("ranged") {
                    anim(DagannothKingsCommon.SUPREME_ATTACK_ANIM)
                    sound(DagannothKingsCommon.SUPREME_ATTACK_SOUND)
                    faceTarget()
                    include(volley())
                }

            phase("combat") {
                weightedSelectorRandom { +random(ranged, weight = 1) }
            }
        }
}
