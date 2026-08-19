package org.rsmod.api.bosses.dsl

import org.rsmod.api.bosses.spec.BossSpec
import org.rsmod.api.bosses.spec.Effect

fun dragon(
    npcType: String,
    meleeMax: Int,
    dragonfireMax: Int = 50,
    ranged: Boolean = false,
    metal: Boolean = false,
    freezeTicks: Int = 0,
    attackRate: Int = 4,
): BossSpec =
    boss(npcType) {
        stats(attackRate = attackRate, aggressionRadius = 8)
        val isRanged = ranged || metal
        val fireType = if (metal) DragonfireMetal else Dragonfire
        val melee =
            ability("melee") {
                anim("seq.dragon_attack")
                hit {
                    damage(0..meleeMax).roll()
                    type(Melee)
                }
            }
        val fire =
            ability("dragonfire") {
                anim("seq.dragon_firebreath_all_attack")
                if (isRanged) {
                    projectile(
                        spotanim = "spotanim.dragon_ranged_fire_attack",
                        travel = "projanim.dragonfire",
                        hit = Effect.Hit(damage = Roll(0..dragonfireMax), type = fireType),
                    )
                } else {
                    include(Effect.Spotanim("spotanim.firebreath_attack", height = 100))
                    hit {
                        damage(0..dragonfireMax).roll()
                        type(fireType)
                    }
                }
                if (freezeTicks > 0) freeze(ticks = freezeTicks, chance = 1, outOf = 3)
            }
        phase("combat") {
            if (isRanged) {
                weightedSelectorRandom {
                    +random(melee, weight = 3, requires = WithinMeleeRange)
                    +random(fire, weight = 2)
                }
            } else {
                weightedSelectorRandom {
                    +random(melee, weight = 5, requires = WithinMeleeRange)
                    +random(fire, weight = 1, requires = WithinMeleeRange)
                }
            }
        }
    }

fun wyvern(
    npcType: String,
    meleeMax: Int,
    iceMax: Int = 50,
    freezeTicks: Int = 11,
    attackRate: Int = 4,
): BossSpec =
    boss(npcType) {
        stats(attackRate = attackRate, aggressionRadius = 8)
        val melee =
            ability("melee") {
                anim("seq.dragon_attack")
                hit {
                    damage(0..meleeMax).roll()
                    type(Melee)
                }
            }
        val ice =
            ability("ice_breath") {
                anim("seq.wyvern_skeleton_iceball")
                projectile(
                    spotanim = "spotanim.wyvern_skeleton_travel_iceball",
                    launch = "spotanim.wyvern_skeleton_launch_iceball",
                    travel = "projanim.dragonfire",
                    hit = Effect.Hit(damage = Roll(0..iceMax), type = WyvernIce),
                )
                freeze(ticks = freezeTicks, chance = 1, outOf = 3)
            }
        phase("combat") {
            weightedSelectorRandom {
                +random(melee, weight = 3, requires = WithinMeleeRange)
                +random(ice, weight = 2)
            }
        }
    }
