package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.AccuracyKind
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.api.random.GameRandom

class Spinolyp
@Inject
constructor(
    deps: BossDeps,
    private val random: GameRandom,
) : BossPluginScript(deps) {

    private fun spines(): Effect =
        projectile(
            spotanim = "spotanim.horror_spines_travel",
            config = SPINE_PROJECTILE,
            hit =
                hit {
                    damage(Accuracy(Roll(0..MAX_HIT)))
                    type(HitType.Ranged)
                    missSpotanim(DagannothKingsCommon.MISS_IMPACT, height = 124)
                    missSound(DagannothKingsCommon.MISS_SOUND)
                    onHit(
                        sequence(
                            poison(6),
                            sound("synth.dagganoth_splash", at = TargetExpr.CurrentTarget),
                        )
                    )
                },
        )

    private fun waterStrike(): Effect =
        projectile(
            spotanim = "spotanim.waterstrike_travel",
            config = WATER_STRIKE_PROJECTILE,
            hit =
                hit {
                    damage(Accuracy(Roll(0..MAX_HIT), kind = AccuracyKind.Ranged))
                    type(HitType.Magic)
                    spotanim("spotanim.waterstrike_impact", height = 124)
                    missSpotanim(DagannothKingsCommon.MISS_IMPACT)
                    missSound(DagannothKingsCommon.MISS_SOUND)
                    onHit(
                        sequence(
                            statDrain("stat.prayer", amount = 1),
                            sound("synth.waterstrike_hit", at = TargetExpr.CurrentTarget),
                        )
                    )
                },
        )

    override val spec =
        boss(
            "npc.dagannoth_water_creature_1x1",
            "npc.dagannoth_water_creature_1x12",
            "npc.dagannoth_water_creature_1x13",
            "npc.dagannoth_water_creature_1x1_invisible",
            "npc.dagannoth_water_creature_1x1_invisible2",
            "npc.dagannoth_water_creature_1x1_invisible3",
        ) {
            stats(attackRate = 4)

            val ranged =
                ability("ranged") {
                    anim("seq.dagannoth_water_creature_attack")
                    sound("synth.dagganoth_spines")
                    faceTarget()
                    include(spines())
                }

            val magic =
                ability("magic") {
                    anim("seq.dagannoth_water_creature_attack")
                    faceTarget()
                    include(waterStrike())
                }

            phase("combat") {
                weightedSelectorRandom {
                    +random(ranged, weight = 1)
                    +random(magic, weight = 1)
                }
            }
        }

    private companion object {
        const val MAX_HIT = 10

        val SPINE_PROJECTILE =
            ProjectileConfig(
                startHeight = 110,
                endHeight = 110,
                startDelay = 10,
                travelTime = 36,
                angle = 25,
                progress = 50,
                stepMultiplier = 5,
            )

        val WATER_STRIKE_PROJECTILE =
            ProjectileConfig(
                startHeight = 172,
                endHeight = 124,
                startDelay = 51,
                travelTime = -5,
                angle = 16,
                progress = 64,
                stepMultiplier = 5,
            )
    }
}
