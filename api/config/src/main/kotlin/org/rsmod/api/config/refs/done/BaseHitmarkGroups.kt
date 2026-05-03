package org.rsmod.api.config.refs.done

import dev.openrune.types.HitmarkTypeGroup

typealias hitmark_groups = BaseHitmarkGroups

object BaseHitmarkGroups {
    val corruption = HitmarkTypeGroup("hitmark.corruption")
    val ironman_blocked = HitmarkTypeGroup("hitmark.ironman_blocked")
    val disease = HitmarkTypeGroup("hitmark.disease")
    val venom = HitmarkTypeGroup("hitmark.venom")
    val heal = HitmarkTypeGroup("hitmark.heal")

    val alt_charge =
        HitmarkTypeGroup(lit = "hitmark.alt_charge_lit", tint = "hitmark.alt_charge_tint")

    val zero_damage =
        HitmarkTypeGroup(lit = "hitmark.zero_damage_lit", tint = "hitmark.zero_damage_tint")

    val alt_uncharge =
        HitmarkTypeGroup(lit = "hitmark.alt_uncharge_lit", tint = "hitmark.alt_uncharge_tint")

    val regular_damage =
        HitmarkTypeGroup(
            lit = "hitmark.regular_damage_lit",
            tint = "hitmark.regular_damage_tint",
            max = "hitmark.regular_damage_max",
        )

    val shield_damage =
        HitmarkTypeGroup(
            lit = "hitmark.shield_damage_lit",
            tint = "hitmark.shield_damage_tint",
            max = "hitmark.shield_damage_max",
        )

    val zalcano_armour_damage =
        HitmarkTypeGroup(
            lit = "hitmark.zalcano_armour_damage_lit",
            tint = "hitmark.zalcano_armour_damage_tint",
            max = "hitmark.zalcano_armour_damage_max",
        )

    val nightmare_totem_charge =
        HitmarkTypeGroup(
            lit = "hitmark.nightmare_totem_charge_lit",
            tint = "hitmark.nightmare_totem_charge_tint",
            max = "hitmark.nightmare_totem_charge_max",
        )

    val nightmare_totem_uncharge =
        HitmarkTypeGroup(
            lit = "hitmark.nightmare_totem_uncharge_lit",
            tint = "hitmark.nightmare_totem_uncharge_tint",
            max = "hitmark.nightmare_totem_uncharge_max",
        )

    val poise_damage =
        HitmarkTypeGroup(
            lit = "hitmark.poise_damage_lit",
            tint = "hitmark.poise_damage_tint",
            max = "hitmark.poise_damage_max",
        )

    val prayer_drain =
        HitmarkTypeGroup(
            lit = "hitmark.prayer_drain_lit",
            tint = "hitmark.prayer_drain_tint",
            max = "hitmark.prayer_drain_max",
        )

    val poison_damage =
        HitmarkTypeGroup(lit = "hitmark.poison_damage_lit", tint = "hitmark.poison_damage_tint")

    val bleed = HitmarkTypeGroup("hitmark.bleed")
    val sanity_drain = HitmarkTypeGroup("hitmark.sanity_drain")
    val sanity_restore = HitmarkTypeGroup("hitmark.sanity_restore")
    val doom = HitmarkTypeGroup("hitmark.doom")
    val burn = HitmarkTypeGroup("hitmark.burn")

    val wintertodt_drain =
        HitmarkTypeGroup(
            lit = "hitmark.wintertodt_drain_lit",
            tint = "hitmark.wintertodt_drain_tint",
        )
}
