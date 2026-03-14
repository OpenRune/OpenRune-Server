package org.rsmod.content.interfaces.bank.configs

import org.rsmod.api.type.refs.comp.ComponentReferences
import org.rsmod.api.type.refs.interf.InterfaceReferences

internal typealias bank_interfaces = BankInterfaces

internal typealias bank_components = BankComponents

internal typealias bank_comsubs = BankSubComponents

object BankInterfaces : InterfaceReferences() {
    val tutorial_overlay = inter("screenhighlight")
}

object BankComponents : ComponentReferences() {
    val tutorial_button = component("bankmain:bank_tut")
    val capacity_container = component("bankmain:capacity_layer")
    val capacity_text = component("bankmain:capacity")
    val main_inventory = component("bankmain:items")
    val tabs = component("bankmain:tabs")
    val incinerator_confirm = component("bankmain:incinerator_confirm")
    val potionstore_items = component("bankmain:potionstore_items")
    val worn_off_stab = component("bankmain:stabatt")
    val worn_off_slash = component("bankmain:slashatt")
    val worn_off_crush = component("bankmain:crushatt")
    val worn_off_magic = component("bankmain:magicatt")
    val worn_off_range = component("bankmain:rangeatt")
    val worn_speed_base = component("bankmain:attackspeedbase")
    val worn_speed = component("bankmain:attackspeedactual")
    val worn_def_stab = component("bankmain:stabdef")
    val worn_def_slash = component("bankmain:slashdef")
    val worn_def_crush = component("bankmain:crushdef")
    val worn_def_range = component("bankmain:rangedef")
    val worn_def_magic = component("bankmain:magicdef")
    val worn_melee_str = component("bankmain:meleestrength")
    val worn_ranged_str = component("bankmain:rangestrength")
    val worn_magic_dmg = component("bankmain:magicdamage")
    val worn_prayer = component("bankmain:prayer")
    val worn_undead = component("bankmain:typemultiplier")
    val worn_slayer = component("bankmain:slayermultiplier")
    val tutorial_overlay_target = component("bankmain:bank_highlight")
    val confirmation_overlay_target = component("bankmain:popup")
    val tooltip = component("bankmain:tooltip")

    val rearrange_mode_swap = component("bankmain:swap")
    val rearrange_mode_insert = component("bankmain:insert")
    val withdraw_mode_item = component("bankmain:item")
    val withdraw_mode_note = component("bankmain:note")
    val always_placehold = component("bankmain:placeholder")
    val deposit_inventory = component("bankmain:depositinv")
    val deposit_worn = component("bankmain:depositworn")
    val quantity_1 = component("bankmain:quantity1")
    val quantity_5 = component("bankmain:quantity5")
    val quantity_10 = component("bankmain:quantity10")
    val quantity_x = component("bankmain:quantityx")
    val quantity_all = component("bankmain:quantityall")

    val incinerator_toggle = component("bankmain:incinerator_toggle")
    val tutorial_button_toggle = component("bankmain:banktut_toggle")
    val inventory_item_options_toggle = component("bankmain:sideops_toggle")
    val deposit_inv_toggle = component("bankmain:depositinv_toggle")
    val deposit_worn_toggle = component("bankmain:depositworn_toggle")
    val release_placehold = component("bankmain:release_placeholders")
    val bank_fillers_1 = component("bankmain:bank_filler_1")
    val bank_fillers_10 = component("bankmain:bank_filler_10")
    val bank_fillers_50 = component("bankmain:bank_filler_50")
    val bank_fillers_x = component("bankmain:bank_filler_x")
    val bank_fillers_all = component("bankmain:bank_filler_all")
    val bank_fillers_fill = component("bankmain:bank_filler_confirm")
    val bank_tab_display = component("bankmain:dropdown_content")

    val side_inventory = component("bankside:items")
    val worn_inventory = component("bankside:wornops")
    val lootingbag_inventory = component("bankside:lootingbag_items")
    val league_inventory = component("bankside:league_secondinv_items")
    val bankside_highlight = component("bankside:bankside_highlight")

    val tutorial_close_button = component("screenhighlight:pausebutton")
    val tutorial_next_page = component("screenhighlight:continue")
    val tutorial_prev_page = component("screenhighlight:previous")
}

@Suppress("ConstPropertyName")
object BankSubComponents {
    const val main_tab = 10
    val other_tabs = 11..19

    val tab_extended_slots_offset = 19..28
}
