package org.rsmod.content.bosses.dagannothkings

import org.rsmod.api.bosses.dsl.area
import org.rsmod.api.bosses.spec.ProjectileConfig
import org.rsmod.api.bosses.spec.TargetExpr

internal object DagannothKingsCommon {
    const val REX = "npc.dagcave_melee_boss"
    const val PRIME = "npc.dagcave_magic_boss"
    const val SUPREME = "npc.dagcave_ranged_boss"

    const val ATTACK_RATE = 4

    const val REX_MAX_HIT = 26
    const val PRIME_MAX_HIT = 50
    const val SUPREME_MAX_HIT = 30

    const val REX_ATTACK_ANIM = "seq.dagannoth_meganoth_attack_melee"
    const val PRIME_ATTACK_ANIM = "seq.dagannoth_meganoth_attack_mage"
    const val SUPREME_ATTACK_ANIM = "seq.dagannoth_meganoth_attack_range"

    const val PRIME_PROJECTILE = "spotanim.waterwave_travel"
    const val PRIME_IMPACT = "spotanim.waterwave_impact"
    const val SUPREME_PROJECTILE = "spotanim.dagannoth_spine_spotanim_travel"

    const val REX_ATTACK_SOUND = "synth.meganoth_melee_attack"
    const val REX_HIT_SOUND = "synth.meganoth_hit"
    const val PRIME_ATTACK_SOUND = "synth.waterwave_cast_and_fire"
    const val PRIME_HIT_SOUND = "synth.waterwave_hit"
    const val SUPREME_ATTACK_SOUND = "synth.meganoth_spines"
    const val SUPREME_HIT_SOUND = "synth.meganoth_hit"

    val PRIME_AOE =
        area(
            TargetExpr.Offset(TargetExpr.CurrentTargetTile, -1, -1),
            TargetExpr.Offset(TargetExpr.CurrentTargetTile, 1, 1),
        )

    val SUPREME_AREA =
        area(
            TargetExpr.Offset(TargetExpr.CurrentTargetTile, -3, -3),
            TargetExpr.Offset(TargetExpr.CurrentTargetTile, 3, 3),
        )

    val PRIME_PROJECTILE_CONFIG =
        ProjectileConfig(
            startHeight = 220,
            endHeight = 100,
            startDelay = 10,
            travelTime = 56,
            angle = 20,
            progress = 50,
            stepMultiplier = 5,
        )

    val SUPREME_PROJECTILE_CONFIG =
        ProjectileConfig(
            startHeight = 210,
            endHeight = 100,
            startDelay = 10,
            travelTime = 46,
            angle = 40,
            progress = 50,
            stepMultiplier = 5,
        )
}
