package org.rsmod.content.interfaces.equipment.configs

import dev.openrune.component
import dev.openrune.inter

typealias equip_components = EquipmentTabComponents

typealias equip_interfaces = EquipmentTabInterfaces

object EquipmentTabComponents {
    val equipment = component("wornitems:equipment")
    val guide_prices = component("wornitems:pricechecker")
    val items_kept_on_death = component("wornitems:deathkeep")
    val call_follower = component("wornitems:call_follower")

    val equipment_stats_side_inv = component("equipment_side:items")
    val equipment_stats_off_stab = component("equipment:stabatt")
    val equipment_stats_off_slash = component("equipment:slashatt")
    val equipment_stats_off_crush = component("equipment:crushatt")
    val equipment_stats_off_magic = component("equipment:magicatt")
    val equipment_stats_off_range = component("equipment:rangeatt")
    val equipment_stats_speed_base = component("equipment:attackspeedbase")
    val equipment_stats_speed = component("equipment:attackspeedactual")
    val equipment_stats_def_stab = component("equipment:stabdef")
    val equipment_stats_def_slash = component("equipment:slashdef")
    val equipment_stats_def_crush = component("equipment:crushdef")
    val equipment_stats_def_range = component("equipment:magicdef")
    val equipment_stats_def_magic = component("equipment:rangedef")
    val equipment_stats_melee_str = component("equipment:meleestrength")
    val equipment_stats_ranged_str = component("equipment:rangestrength")
    val equipment_stats_magic_dmg = component("equipment:magicdamage")
    val equipment_stats_prayer = component("equipment:prayer")
    val equipment_stats_undead = component("equipment:typemultiplier")
    val equipment_stats_undead_tooltip = component("equipment:tooltip")
    val equipment_stats_slayer = component("equipment:slayermultiplier")

    val guide_prices_side_inv = component("ge_pricechecker_side:items")
    val guide_prices_main_inv = component("ge_pricechecker:items")
    val guide_prices_search = component("ge_pricechecker:other")
    val guide_prices_search_obj = component("ge_pricechecker:otheritem")
    val guide_prices_add_all = component("ge_pricechecker:all")
    val guide_prices_total_price_text = component("ge_pricechecker:output")

    val items_kept_on_death_pbutton = component("deathkeep:right")
    val items_kept_on_death_risk = component("deathkeep:value")
}

object EquipmentTabInterfaces {
    val equipment_stats_main = inter("equipment")
    val equipment_stats_side = inter("equipment_side")
    val guide_prices_main = inter("ge_pricechecker")
    val guide_prices_side = inter("ge_pricechecker_side")
    val deathkeep = inter("deathkeep")
}
