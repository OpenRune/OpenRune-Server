package org.rsmod.content.other.treasuretrails

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.*
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.combat.commons.types.MeleeAttackType

class AncientWizardMagic @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss("npc.trail_zaros_wizard") {
            stats(attackRate = ATTACK_RATE)
            val blitz =
                ability("blitz") {
                    anim("seq.zaros_casting")
                    projectile {
                        spotanim = "spotanim.blood_blitz_travel"
                        config =
                            ProjectileConfig.fixed(
                                startHeight = 43,
                                endHeight = 0,
                                delay = 51,
                                travel = 25,
                                angle = 16,
                                progress = 64,
                            )
                        hit {
                            damage(Accuracy(Roll(0..MAGIC_MAX_HIT)))
                            type(Magic)
                            spotanim("spotanim.firestrike_impact", height = IMPACT_HEIGHT)
                            missSpotanim("spotanim.failedspell_impact")
                            missSound("synth.spellfail")
                        }
                    }
                }
            phase("combat") { weightedSelectorRandom { +random(blitz, weight = 1) } }
        }

    private companion object {
        const val MAGIC_MAX_HIT = 18
        const val IMPACT_HEIGHT = 124
    }
}

class AncientWizardRanged @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss("npc.trail_zaros_range") {
            stats(attackRate = ATTACK_RATE)
            val bolt =
                ability("bolt") {
                    anim("seq.xbows_human_fire_and_reload")
                    projectile {
                        spotanim = "spotanim.crossbowbolt_travel"
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
                            damage(Accuracy(Roll(0..RANGED_MAX_HIT)))
                            type(Ranged)
                        }
                    }
                }
            phase("combat") { weightedSelectorRandom { +random(bolt, weight = 1) } }
        }

    private companion object {
        const val RANGED_MAX_HIT = 23
    }
}

class AncientWizardMelee @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {
    override val spec =
        boss("npc.trail_zaros_melee") {
            stats(attackRate = ATTACK_RATE)
            val lunge =
                ability("lunge") {
                    anim("seq.human_ddagger_lunge")
                    hit {
                        damage(
                            Accuracy(
                                Roll(0..MELEE_MAX_HIT),
                                meleeAttackType = MeleeAttackType.Stab,
                            )
                        )
                        type(Melee)
                        delay = 1
                        // UNVERIFIED: poison odds; only the starting damage of 8 is known.
                        onHit(POISON)
                    }
                }
            phase("combat") { weightedSelectorRandom { +random(lunge, weight = 1) } }
        }

    private companion object {
        const val MELEE_MAX_HIT = 36
        const val POISON_DAMAGE = 8
        val POISON = poison(POISON_DAMAGE, chance = 1, outOf = 4)
    }
}

private const val ATTACK_RATE = 4
