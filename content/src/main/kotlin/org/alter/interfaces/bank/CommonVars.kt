package org.alter.interfaces.bank

import org.alter.api.ext.boolVarBit
import org.alter.api.ext.enumVarBit
import org.alter.api.ext.intVarBit
import org.alter.game.model.entity.Player


var Player.selectedTab by enumVarBit<BankTab>("varbits.bank_currenttab")

var Player.insertMode by boolVarBit("varbits.bank_insertmode")
var Player.withdrawCert by boolVarBit("varbits.bank_withdrawnotes")
var Player.alwaysPlacehold by boolVarBit("varbits.bank_leaveplaceholders")
var Player.lastQtyInput by intVarBit("varbits.bank_requestedquantity")
var Player.leftClickQtyMode by enumVarBit<QuantityMode>("varbits.bank_quantity_type")

var Player.tabDisplayMode by enumVarBit<TabDisplayMode>("varbits.bank_tab_display")
var Player.incinerator by boolVarBit("varbits.bank_showincinerator")
var Player.tutorialButton by boolVarBit("varbits.bank_hidebanktut")
var Player.invItemOptions by boolVarBit("varbits.bank_hidesideops")
var Player.depositInvButton by boolVarBit("varbits.bank_hidedepositinv")
var Player.depositWornButton by boolVarBit("varbits.bank_hidedepositworn")
var Player.bankFillerMode by enumVarBit<BankFillerMode>("varbits.bank_fillermode")

internal var Player.disableIfEvents by boolVarBit("varbits.bank_disable_ifevents")

