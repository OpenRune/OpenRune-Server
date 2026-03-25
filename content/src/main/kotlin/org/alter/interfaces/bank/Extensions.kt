package org.alter.interfaces.bank

import dev.openrune.definition.type.widget.IfEvent
import org.alter.game.model.entity.Player
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
