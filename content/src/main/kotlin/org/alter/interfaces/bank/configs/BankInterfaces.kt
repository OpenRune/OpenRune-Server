package org.alter.interfaces.bank.configs

internal typealias bank_interfaces = BankInterfaces

internal typealias bank_components = BankComponents

internal typealias bank_comsubs = BankSubComponents

object BankInterfaces {
    val tutorial_overlay = "interfaces.screenhighlight"
}

object BankComponents {
    val tutorial_button = "components.bankmain:bank_tut"
    val capacity_container = "components.bankmain:capacity_layer"
    val capacity_text = "components.bankmain:capacity"
    val main_inventory = "components.bankmain:items"
    val tabs = "components.bankmain:tabs"
    val incinerator_confirm = "components.bankmain:incinerator_confirm"
    val potionstore_items = "components.bankmain:potionstore_items"
    val worn_off_stab = "components.bankmain:stabatt"
    val worn_off_slash = "components.bankmain:slashatt"
    val worn_off_crush = "components.bankmain:crushatt"
    val worn_off_magic = "components.bankmain:magicatt"
    val worn_off_range = "components.bankmain:rangeatt"
    val worn_speed_base = "components.bankmain:attackspeedbase"
    val worn_speed = "components.bankmain:attackspeedactual"
    val worn_def_stab = "components.bankmain:stabdef"
    val worn_def_slash = "components.bankmain:slashdef"
    val worn_def_crush = "components.bankmain:crushdef"
    val worn_def_range = "components.bankmain:rangedef"
    val worn_def_magic = "components.bankmain:magicdef"
    val worn_melee_str = "components.bankmain:meleestrength"
    val worn_ranged_str = "components.bankmain:rangestrength"
    val worn_magic_dmg = "components.bankmain:magicdamage"
    val worn_prayer = "components.bankmain:prayer"
    val worn_undead = "components.bankmain:typemultiplier"
    val worn_slayer = "components.bankmain:slayermultiplier"
    val tutorial_overlay_target = "components.bankmain:bank_highlight"
    val confirmation_overlay_target = "components.bankmain:popup"
    val tooltip = "components.bankmain:tooltip"

    val rearrange_mode_swap = "components.bankmain:swap"
    val rearrange_mode_insert = "components.bankmain:insert"
    val withdraw_mode_item = "components.bankmain:item"
    val withdraw_mode_note = "components.bankmain:note"
    val always_placehold = "components.bankmain:placeholder"
    val deposit_inventory = "components.bankmain:depositinv"
    val deposit_worn = "components.bankmain:depositworn"
    val quantity_1 = "components.bankmain:quantity1"
    val quantity_5 = "components.bankmain:quantity5"
    val quantity_10 = "components.bankmain:quantity10"
    val quantity_x = "components.bankmain:quantityx"
    val quantity_all = "components.bankmain:quantityall"

    val incinerator_toggle = "components.bankmain:incinerator_toggle"
    val tutorial_button_toggle = "components.bankmain:banktut_toggle"
    val inventory_item_options_toggle = "components.bankmain:sideops_toggle"
    val deposit_inv_toggle = "components.bankmain:depositinv_toggle"
    val deposit_worn_toggle = "components.bankmain:depositworn_toggle"
    val release_placehold = "components.bankmain:release_placeholders"
    val bank_fillers_1 = "components.bankmain:bank_filler_1"
    val bank_fillers_10 = "components.bankmain:bank_filler_10"
    val bank_fillers_50 = "components.bankmain:bank_filler_50"
    val bank_fillers_x = "components.bankmain:bank_filler_x"
    val bank_fillers_all = "components.bankmain:bank_filler_all"
    val bank_fillers_fill = "components.bankmain:bank_filler_confirm"
    val bank_tab_display = "components.bankmain:dropdown_content"

    val side_inventory = "components.bankside:items"
    val worn_inventory = "components.bankside:wornops"
    val lootingbag_inventory = "components.bankside:lootingbag_items"
    val league_inventory = "components.bankside:league_secondinv_items"
    val bankside_highlight = "components.bankside:bankside_highlight"

    val tutorial_close_button = "components.screenhighlight:pausebutton"
    val tutorial_next_page = "components.screenhighlight:continue"
    val tutorial_prev_page = "components.screenhighlight:previous"
}

@Suppress("ConstPropertyName")
object BankSubComponents {
    const val main_tab = 10
    val other_tabs = 11..19

    val tab_extended_slots_offset = 19..28
}
