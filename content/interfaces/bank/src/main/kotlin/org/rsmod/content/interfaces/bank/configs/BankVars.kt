package org.rsmod.content.interfaces.bank.configs

import org.rsmod.api.type.builders.varbit.VarBitBuilder
import org.rsmod.api.type.builders.varp.VarpBuilder
import org.rsmod.api.type.refs.varbit.VarBitReferences
import org.rsmod.api.type.refs.varp.VarpReferences

internal typealias bank_varbits = BankVarBits

object BankVarBits : VarBitReferences() {
    val rearrange_mode = varBit("bank_insertmode")
    val withdraw_mode = varBit("bank_withdrawnotes")
    val placeholders = varBit("bank_leaveplaceholders")
    val last_quantity_input = varBit("bank_requestedquantity")
    val left_click_quantity = varBit("bank_quantity_type")
    val bank_filler_quantity = varBit("bank_fillermode")
    val tab_display = varBit("bank_tab_display")
    val incinerator = varBit("bank_showincinerator")
    val tutorial_button = varBit("bank_hidebanktut")
    val inventory_item_options = varBit("bank_hidesideops")
    val deposit_inventory_button = varBit("bank_hidedepositinv")
    val deposit_worn_items_button = varBit("bank_hidedepositworn")
    val always_deposit_to_potion_store = varBit("bank_depositpotion")
    val tutorial_current_page = varBit("hnt_hint_step")
    val tutorial_total_pages = varBit("hnt_hint_max_step")

    val tab_size1 = varBit("bank_tab_1")
    val tab_size2 = varBit("bank_tab_2")
    val tab_size3 = varBit("bank_tab_3")
    val tab_size4 = varBit("bank_tab_4")
    val tab_size5 = varBit("bank_tab_5")
    val tab_size6 = varBit("bank_tab_6")
    val tab_size7 = varBit("bank_tab_7")
    val tab_size8 = varBit("bank_tab_8")
    val tab_size9 = varBit("bank_tab_9")
    val tab_size_main = varBit("bank_tab_main")

    val selected_tab = varBit("bank_currenttab")

    val disable_ifevents = varBit("bank_disable_ifevents")
}

internal object BankVarBitBuilder : VarBitBuilder() {
    init {
        build("bank_tab_main") {
            baseVar = BankVarps.bank_serverside_vars
            startBit = 0
            endBit = 12
        }
        build("bank_disable_ifevents") {
            baseVar = BankVarps.bank_serverside_vars
            startBit = 13
            endBit = 13
        }
    }
}

object BankVarps : VarpReferences() {
    val bank_serverside_vars = varp("bank_serverside_vars")
}

internal object BankVarpBuilder : VarpBuilder() {
    init {
        build("bank_serverside_vars")
    }
}
