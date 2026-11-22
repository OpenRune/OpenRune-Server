package org.rsmod.content.bosses.dagannothkings

import jakarta.inject.Inject
import org.rsmod.api.bosses.dsl.Accuracy
import org.rsmod.api.bosses.dsl.MeleeAttackType
import org.rsmod.api.bosses.dsl.Roll
import org.rsmod.api.bosses.dsl.anim
import org.rsmod.api.bosses.dsl.boss
import org.rsmod.api.bosses.dsl.hit
import org.rsmod.api.bosses.dsl.projectile
import org.rsmod.api.bosses.dsl.sound
import org.rsmod.api.bosses.dsl.soundTo
import org.rsmod.api.bosses.runtime.BossDeps
import org.rsmod.api.bosses.runtime.BossPluginScript
import org.rsmod.api.bosses.spec.Effect
import org.rsmod.api.bosses.spec.HitType
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TargetExpr

class WaterbirthWallasalki @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private fun cast(): Effect =
        projectile(
            spotanim = "spotanim.waterwave_travel",
            config = WALLASALKI_PROJECTILE,
            hit =
                hit {
                    damage(Accuracy(Roll(0..MAX_HIT)))
                    type(HitType.Magic)
                    spotanim("spotanim.waterwave_impact", height = 124)
                    missSpotanim("spotanim.failedspell_impact")
                    missSound("synth.spellfail")
                    onHit(sound("synth.waterwave_hit", at = TargetExpr.CurrentTarget))
                },
        )

    override val spec =
        boss(
            "npc.dungeon_dagganoth_magic_monster",
            "npc.dungeon_dagganoth_magic_monster_deeper",
        ) {
            stats(attackRate = ATTACK_RATE)

            val magic =
                ability("magic") {
                    anim("seq.wallasalki_attack")
                    sound("synth.waterwave_cast_and_fire")
                    faceTarget()
                    include(cast())
                }

            phase("combat") { weightedSelectorRandom { +random(magic, weight = 1) } }
        }

    private companion object {
        const val ATTACK_RATE = 6
        const val MAX_HIT = 30
    }
}

class WaterbirthRangedDagannoth @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    private fun shot(): Effect =
        projectile(
            spotanim = "spotanim.horror_spines_travel",
            config = DAGANNOTH_PROJECTILE,
            hit =
                hit {
                    damage(Accuracy(Roll(0..MAX_HIT)))
                    type(HitType.Ranged)
                    missSpotanim("spotanim.failedspell_impact", height = 124)
                    missSound("synth.spellfail")
                    onHit(sound("synth.dagganoth_hit", at = TargetExpr.CurrentTarget))
                },
        )

    override val spec =
        boss(
            "npc.dungeon_dagganoth_lighter",
            "npc.dungeon_dagganoth_lighter_deeper",
        ) {
            stats(attackRate = ATTACK_RATE)

            val ranged =
                ability("ranged") {
                    anim("seq.horror_dagannoth_rangeattack")
                    sound("synth.dagganoth_spines")
                    faceTarget()
                    include(shot())
                }

            phase("combat") { weightedSelectorRandom { +random(ranged, weight = 1) } }
        }

    private companion object {
        const val ATTACK_RATE = 3
        const val MAX_HIT = 20
    }
}

class WaterbirthMeleeDagannoth @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    override val spec =
        boss(
            "npc.dungeon_dagganoth_mainroom",
            "npc.dungeon_dagganoth_mainroom_deeper",
        ) {
            stats(attackRate = ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim("seq.horror_dagannoth_attack")
                    soundTo("synth.dagganoth_attack")
                    faceTarget()
                    hit {
                        damage(
                            Accuracy(
                                Roll(0..MAX_HIT),
                                meleeAttackType = MeleeAttackType.Stab,
                            )
                        )
                        type(HitType.Melee)
                        delay = 1
                        onHit(sound("synth.dagganoth_hit", at = TargetExpr.CurrentTarget))
                    }
                }

            phase("combat") { weightedSelectorRandom { +random(melee, weight = 1) } }
        }

    private companion object {
        const val ATTACK_RATE = 4
        const val MAX_HIT = 15
    }
}

class WaterbirthRockLobster @Inject constructor(deps: BossDeps) : BossPluginScript(deps) {

    override val spec =
        boss("npc.dagannoth_rock_lobster_2x2") {
            stats(attackRate = ATTACK_RATE)

            val melee =
                ability("melee") {
                    anim("seq.dagannoth_rock_lobster_attack")
                    soundTo("synth.rock_lobster_attack")
                    faceTarget()
                    hit {
                        damage(
                            Accuracy(
                                Roll(0..MAX_HIT),
                                meleeAttackType = MeleeAttackType.Crush,
                            )
                        )
                        type(HitType.Melee)
                        delay = 1
                        onHit(sound("synth.rock_lobster_hit", at = TargetExpr.CurrentTarget))
                    }
                }

            phase("combat") { weightedSelectorRandom { +random(melee, weight = 1) } }
        }

    private companion object {
        const val ATTACK_RATE = 2
        const val MAX_HIT = 11
    }
}

private val WALLASALKI_PROJECTILE =
    ProjectileConfig(
        startHeight = 100,
        endHeight = 100,
        startDelay = 10,
        travelTime = 36,
        angle = 40,
        progress = 50,
        stepMultiplier = 5,
    )

private val DAGANNOTH_PROJECTILE =
    ProjectileConfig(
        startHeight = 100,
        endHeight = 100,
        startDelay = 10,
        travelTime = 36,
        angle = 40,
        progress = 50,
        stepMultiplier = 5,
    )
