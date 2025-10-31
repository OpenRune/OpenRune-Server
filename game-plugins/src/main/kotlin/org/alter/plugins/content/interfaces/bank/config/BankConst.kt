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
    val WITHDRAW_NOTES = "varbits.bank_withdrawnotes"
    val INSERTMODE = "varbits.bank_insertmode"
    val LEAVEPLACEHOLDERS = "varbits.bank_leaveplaceholders"
    val REQUESTEDQUANTITY = "varbits.bank_requestedquantity"
    val QUANITY_TYPE = "varbits.bank_quantity_type"
    val SHOW_INCINERATOR = "varbits.bank_showincinerator"
    val CURRENTTAB =  "varbits.bank_currenttab"
    val TAB_DISPLAY = "varbits.bank_tab_display"
}