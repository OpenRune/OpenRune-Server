package org.alter.plugins.content.interfaces.bank.config

import org.alter.rscm.RSCM.asRSCM

object Interfaces {
    val BANK_MAIN = "interfaces.bankmain".asRSCM()
    val BANKSIDE = "interfaces.bankside".asRSCM()
}

object Components {
    val BANK_MAINTAB_COMPONENT = "components.bankmain:com_51".asRSCM()
    val BANKSIDE_CHILD = "components.bankside:prepot_device_loadout_2_save".asRSCM()
    val BACK_CAPACITY = "components.bankmain:menu_container".asRSCM()
    val TITLE = "components.bankmain:incinerator_toggle".asRSCM()
    val DEPOSIT_WORN = "components.bankmain:quantity1_text".asRSCM()
    val SWAP = "components.bankmain:slashdef".asRSCM()
    val TABS = "components.bankmain:item".asRSCM()
    val TUT = "components.bankmain:quantity10".asRSCM()
}

object Varbits {
    val WITHDRAW_NOTES = "varbits.bank_withdrawnotes".asRSCM()
    val INSERTMODE = "varbits.bank_insertmode".asRSCM()
    val LEAVEPLACEHOLDERS = "varbits.bank_leaveplaceholders".asRSCM()
    val REQUESTEDQUANTITY = "varbits.bank_requestedquantity".asRSCM()
    val QUANITY_TYPE = "varbits.bank_quantity_type".asRSCM()
    val SHOW_INCINERATOR = "varbits.bank_showincinerator".asRSCM()
    val CURRENTTAB =  "varbits.bank_currenttab".asRSCM()
    val TAB_DISPLAY = "varbits.bank_tab_display".asRSCM()
}