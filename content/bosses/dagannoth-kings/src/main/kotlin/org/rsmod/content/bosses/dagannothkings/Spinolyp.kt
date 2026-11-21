package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.AccuracyKind
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig

class Spinolyp @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private fun spines(): Effect =
        projectile(
            spotanim = "spotanim.horror_spines_travel",
            config = SPINE_PROJECTILE,
            hit =
                hit {
                    damage(Accuracy(NpcMaxHit()))
                    type(HitType.Ranged)
                    onHit(poison(6))
                },
        )

    private fun waterStrike(): Effect =
        projectile(
            spotanim = "spotanim.waterstrike_travel",
            config = WATER_STRIKE_PROJECTILE,
            hit =
                hit {
                    damage(Accuracy(NpcMaxHit(), kind = AccuracyKind.Ranged))
                    type(HitType.Magic)
                    spotanim("spotanim.waterstrike_impact", height = 124)
                    onHit(statDrain("stat.prayer", amount = 1))
                },
        )

    override val spec =
        boss(
            "npc.dagannoth_water_creature_1x1",
            "npc.dagannoth_water_creature_1x12",
            "npc.dagannoth_water_creature_1x13",
        ) {
            stats(attackRate = 4)

            val ranged =
                ability("ranged") {
                    anim("seq.dagannoth_water_creature_attack")
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
        val SPINE_PROJECTILE =
            ProjectileConfig(
                startHeight = 110,
                endHeight = 110,
                startDelay = 10,
                travelTime = 46,
                angle = 25,
                progress = 50,
                stepMultiplier = 5,
            )

        val WATER_STRIKE_PROJECTILE =
            ProjectileConfig(
                startHeight = 100,
                endHeight = 100,
                startDelay = 10,
                travelTime = 46,
                angle = 40,
                progress = 50,
                stepMultiplier = 5,
            )
    }
}
