package org.rsmod.content.bosses.kbd

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Effect

class KingBlackDragon @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    override val spec =
        boss("npc.king_dragon") {
            stats(attackRate = 4, aggressionRadius = 8)

            val melee =
                ability("melee") {
                    anim("seq.dragon_attack")
                    hit {
                        damage(0..25).roll()
                        type(Melee)
                    }
                }

            val dragonFire =
                ability("dragonfire") {
                    anim("seq.dragon_firebreath_all_attack")
                    projectile(
                        spotanim = "spotanim.dragon_ranged_fire_attack",
                        travel = "projanim.dragonfire",
                        hit = Effect.Hit(damage = Roll(0..65), type = Dragonfire),
                    )
                }

            val poisonBreath =
                ability("poison_breath") {
                    anim("seq.dragon_firebreath_all_attack")
                    projectile(
                        spotanim = "spotanim.dragon_ranged_toxic_attack",
                        travel = "projanim.dragonfire",
                        hit = Effect.Hit(damage = Roll(0..50), type = Dragonfire),
                    )
                    poison(damage = 8, chance = 1, outOf = 3)
                }

            val freezeBreath =
                ability("freeze_breath") {
                    anim("seq.dragon_firebreath_all_attack")
                    projectile(
                        spotanim = "spotanim.dragon_ranged_ice_attack",
                        travel = "projanim.dragonfire",
                        hit = Effect.Hit(damage = Roll(0..50), type = Dragonfire),
                    )
                    freeze(ticks = 10, chance = 1, outOf = 3)
                }

            val shockBreath =
                ability("shock_breath") {
                    anim("seq.dragon_firebreath_all_attack")
                    projectile(
                        spotanim = "spotanim.dragon_ranged_lightning_attack",
                        travel = "projanim.dragonfire",
                        hit = Effect.Hit(damage = Roll(0..50), type = Dragonfire),
                    )
                    statDrain {
                        +stat("stat.attack") {
                            amount(1)
                            odds(1, 3)
                        }
                        +stat("stat.strength")
                        +stat("stat.defence")
                        +stat("stat.ranged")
                        +stat("stat.magic")
                        amount(2)
                        odds(1, 3)
                    }
                }

            phase("combat") {
                weightedSelectorRandom {
                    +random(melee, weight = 3, requires = WithinMeleeRange)
                    +random(dragonFire, weight = 3)
                    +random(poisonBreath, weight = 1)
                    +random(freezeBreath, weight = 1)
                    +random(shockBreath, weight = 1)
                }
            }
        }
}
