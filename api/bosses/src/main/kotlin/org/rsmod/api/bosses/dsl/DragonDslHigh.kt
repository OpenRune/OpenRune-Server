package org.rsmod.api.bosses.dsl

import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

private const val DRAGON_BREATH_ANIM = "seq.dragon_firebreath_all_attack"
private const val DRAGON_MELEE_ANIM = "seq.dragon_attack"

fun adamantDragon(npcType: String): BossSpec =
    boss(npcType) {
        stats(attackRate = 4, aggressionRadius = 8)
        val melee =
            ability("melee") {
                anim(DRAGON_MELEE_ANIM)
                hit {
                    damage(0..29).roll()
                    type(Melee)
                }
            }
        val meleeFire =
            ability("melee_dragonfire") {
                anim(DRAGON_BREATH_ANIM)
                include(Effect.Spotanim("spotanim.firebreath_attack", height = 100))
                hit {
                    damage(0..50).roll()
                    type(DragonfireMetal)
                }
            }
        val rangedFire =
            ability("ranged_dragonfire") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.dragon_ranged_fire_attack",
                    travel = "projanim.dragonfire",
                    hit = Effect.Hit(damage = Roll(0..50), type = DragonfireMetal),
                )
            }
        val magic =
            ability("magic") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.earthwave_travel",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Roll(0..20), type = Magic),
                )
            }
        val ranged =
            ability("ranged") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.golem_ranged_travel",
                    travel = "projanim.bolt",
                    hit = Effect.Hit(damage = Roll(0..20), type = Ranged),
                )
            }
        val poison =
            ability("poison") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.adamant_dragon_poisonball",
                    travel = "projanim.dragonfire",
                    hit = Effect.Hit(damage = Roll(0..25), type = Typeless),
                )
                poison(damage = 8, chance = 1, outOf = 1)
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 4, requires = WithinMeleeRange)
                +random(meleeFire, weight = 2, requires = WithinMeleeRange)
                +random(rangedFire, weight = 3)
                +random(magic, weight = 3)
                +random(ranged, weight = 3)
                +random(poison, weight = 2)
            }
        }
    }

fun runeDragon(npcType: String): BossSpec =
    boss(npcType) {
        stats(attackRate = 4, aggressionRadius = 8)
        val melee =
            ability("melee") {
                anim(DRAGON_MELEE_ANIM)
                hit {
                    damage(0..29).roll()
                    type(Melee)
                }
            }
        val meleeFire =
            ability("melee_dragonfire") {
                anim(DRAGON_BREATH_ANIM)
                include(Effect.Spotanim("spotanim.firebreath_attack", height = 100))
                hit {
                    damage(0..50).roll()
                    type(DragonfireMetal)
                }
            }
        val rangedFire =
            ability("ranged_dragonfire") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.dragon_ranged_fire_attack",
                    travel = "projanim.dragonfire",
                    hit = Effect.Hit(damage = Roll(0..50), type = DragonfireMetal),
                )
            }
        val magic =
            ability("magic") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.waterwave_travel",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Roll(0..26), type = Magic),
                )
            }
        val ranged =
            ability("ranged") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.golem_ranged_travel",
                    travel = "projanim.bolt",
                    hit = Effect.Hit(damage = Roll(0..31), type = Ranged),
                )
            }
        val spark =
            ability("spark") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.rune_dragon_electovortex",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Roll(0..40), type = Magic),
                )
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 4, requires = WithinMeleeRange)
                +random(meleeFire, weight = 2, requires = WithinMeleeRange)
                +random(rangedFire, weight = 3)
                +random(magic, weight = 3)
                +random(ranged, weight = 3)
                +random(spark, weight = 2)
            }
        }
    }

fun mithrilDragon(npcType: String): BossSpec =
    boss(npcType) {
        stats(attackRate = 4, aggressionRadius = 8)
        val melee =
            ability("melee") {
                anim(DRAGON_MELEE_ANIM)
                hit {
                    damage(0..28).roll()
                    type(Melee)
                }
            }
        val meleeFire =
            ability("melee_dragonfire") {
                anim(DRAGON_BREATH_ANIM)
                include(Effect.Spotanim("spotanim.firebreath_attack", height = 100))
                hit {
                    damage(0..50).roll()
                    type(DragonfireMetal)
                }
            }
        val rangedFire =
            ability("ranged_dragonfire") {
                anim(DRAGON_BREATH_ANIM)
                projectile(
                    spotanim = "spotanim.dragon_ranged_fire_attack",
                    travel = "projanim.dragonfire",
                    hit = Effect.Hit(damage = Roll(0..50), type = DragonfireMetal),
                )
            }
        val magic =
            ability("magic") {
                anim("seq.brut_dragon_casting")
                projectile(
                    spotanim = "spotanim.waterblast_travel",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Roll(0..18), type = Magic),
                )
            }
        val magicalRanged =
            ability("magical_ranged") {
                anim("seq.brut_dragon_casting")
                projectile(
                    spotanim = "spotanim.ice_arrow_travel",
                    travel = "projanim.magic_spell",
                    hit = Effect.Hit(damage = Roll(0..18), type = Magic),
                )
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 4, requires = WithinMeleeRange)
                +random(meleeFire, weight = 2, requires = WithinMeleeRange)
                +random(rangedFire, weight = 3)
                +random(magic, weight = 3)
                +random(magicalRanged, weight = 3)
            }
        }
    }
