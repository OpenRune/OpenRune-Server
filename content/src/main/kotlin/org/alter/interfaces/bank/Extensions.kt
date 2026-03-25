package org.alter.interfaces.bank

import dev.openrune.definition.type.widget.IfEvent
import org.alter.game.model.entity.Player
import org.alter.interfaceInvInit
import org.alter.interfaces.bank.BankState.bankActiveTab
import org.alter.interfaces.bank.BankState.bankSearchMode
import org.alter.interfaces.ifOpenMainSidePair
import org.alter.interfaces.ifSetEvents

fun Player.openBank() {
    val bankInv = invMap.getValue("inv.bank")

    // Transmit bank and player inventory to client
    startInvTransmit(bankInv)
    startInvTransmit(inventory)

    // Open the bank main + side panel
    ifOpenMainSidePair("interfaces.bankmain", "interfaces.bankside")

    // Initialize bank main item container with withdraw option labels
    interfaceInvInit(
        player = this,
        inv = bankInv,
        target = "components.bankmain:items",
        objRowCount = 8,
        objColCount = 1,
        dragType = 1,
        dragComponent = "components.bankmain:items",
        op1 = "Withdraw-1",
        op2 = "Withdraw-5",
        op3 = "Withdraw-10",
        op4 = "Withdraw-X",
        op5 = "Withdraw-All",
    )

    // Initialize bank side inventory with deposit option labels
    interfaceInvInit(
        player = this,
        inv = inventory,
        target = "components.bankside:items",
        objRowCount = 4,
        objColCount = 7,
        op1 = "Deposit-1",
        op2 = "Deposit-5",
        op3 = "Deposit-10",
        op4 = "Deposit-X",
        op5 = "Deposit-All",
    )

    // Bank items: withdraw options (Op1-Op8), examine (Op10), drag
    ifSetEvents(
        "components.bankmain:items",
        0..799,
        IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4,
        IfEvent.Op5, IfEvent.Op6, IfEvent.Op7, IfEvent.Op8,
        IfEvent.Op10,
        IfEvent.DragTarget,
    )

    // Side panel inventory: deposit options (Op1-Op5), examine (Op10)
    ifSetEvents(
        "components.bankside:items",
        0..27,
        IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4,
        IfEvent.Op5,
        IfEvent.Op10,
    )

    // Tab headers: click to switch
    ifSetEvents(
        "components.bankmain:tabs",
        0..9,
        IfEvent.Op1,
    )

    // Reset transient state
    bankActiveTab = 0
    bankSearchMode = false
}

fun Player.closeBank() {
    val bankInv = invMap.getValue("inv.bank")
    stopInvTransmit(bankInv)
    bankSearchMode = false
}
