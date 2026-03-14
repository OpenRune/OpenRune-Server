package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.hitmark.HitmarkReferences

typealias hitmarks = BaseHitmarks

object BaseHitmarks : HitmarkReferences() {
    val corruption = hitmark("corruption")
    val ironman_blocked = hitmark("ironman_blocked")
    val disease = hitmark("disease")
    val venom = hitmark("venom")
    val heal = hitmark("heal")

    val alt_charge_lit = hitmark("alt_charge_lit")
    val alt_charge_tint = hitmark("alt_charge_tint")

    val zero_damage_lit = hitmark("zero_damage_lit")
    val zero_damage_tint = hitmark("zero_damage_tint")

    val alt_uncharge_lit = hitmark("alt_uncharge_lit")
    val alt_uncharge_tint = hitmark("alt_uncharge_tint")

    val regular_damage_lit = hitmark("regular_damage_lit")
    val regular_damage_tint = hitmark("regular_damage_tint")
    val regular_damage_max = hitmark("regular_damage_max")

    val shield_damage_lit = hitmark("shield_damage_lit")
    val shield_damage_tint = hitmark("shield_damage_tint")
    val shield_damage_max = hitmark("shield_damage_max")

    val zalcano_armour_damage_lit = hitmark("zalcano_armour_damage_lit")
    val zalcano_armour_damage_tint = hitmark("zalcano_armour_damage_tint")
    val zalcano_armour_damage_max = hitmark("zalcano_armour_damage_max")

    val nightmare_totem_charge_lit = hitmark("nightmare_totem_charge_lit")
    val nightmare_totem_charge_tint = hitmark("nightmare_totem_charge_tint")
    val nightmare_totem_charge_max = hitmark("nightmare_totem_charge_max")

    val nightmare_totem_uncharge_lit = hitmark("nightmare_totem_uncharge_lit")
    val nightmare_totem_uncharge_tint = hitmark("nightmare_totem_uncharge_tint")
    val nightmare_totem_uncharge_max = hitmark("nightmare_totem_uncharge_max")

    val poise_damage_lit = hitmark("poise_damage_lit")
    val poise_damage_tint = hitmark("poise_damage_tint")
    val poise_damage_max = hitmark("poise_damage_max")

    val prayer_drain_lit = hitmark("prayer_drain_lit")
    val prayer_drain_tint = hitmark("prayer_drain_tint")
    val prayer_drain_max = hitmark("prayer_drain_max")

    val poison_damage_lit = hitmark("poison_damage_lit")
    val poison_damage_tint = hitmark("poison_damage_tint")

    val bleed = hitmark("bleed")
    val sanity_drain = hitmark("sanity_drain")
    val sanity_restore = hitmark("sanity_restore")
    val doom = hitmark("doom")
    val burn = hitmark("burn")

    val wintertodt_drain_lit = hitmark("wintertodt_drain_lit")
    val wintertodt_drain_tint = hitmark("wintertodt_drain_tint")
}
