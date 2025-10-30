package org.alter.plugins.content.interfaces.bank.config

import org.alter.rscm.RSCM.asRSCM

object Interfaces {
    val BANK_MAIN = "interfaces.bankmain".asRSCM()
    val BANKSIDE = "interfaces.bankside".asRSCM()
}

object Components {
    val BANK_MAINTAB_COMPONENT = "components.bankmain:items".asRSCM()
    val BANKSIDE_CHILD = "components.bankside:items_container".asRSCM()
    val BACK_CAPACITY = "components.bankmain:capacity".asRSCM()
    val TITLE = "components.bankmain:title".asRSCM()
    val DEPOSIT_WORN = "components.bankmain:depositworn".asRSCM()
    val SWAP = "components.bankmain:swap".asRSCM()
    val TABS = "components.bankmain:tabs".asRSCM()
    val TUT = "components.bankmain:bank_tut".asRSCM()
    val DEPOSITINV = "components.bankmain:depositinv".asRSCM()
    val PLACEHOLDER = "components.bankmain:placeholder".asRSCM()
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