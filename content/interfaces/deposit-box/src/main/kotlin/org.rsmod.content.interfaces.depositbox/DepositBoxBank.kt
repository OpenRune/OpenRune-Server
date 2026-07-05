package org.rsmod.content.interfaces.depositbox

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.BankTab
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.interfaces.bank.selectedTab
import org.rsmod.game.inv.Inventory

/** A deposit box only deposits into the main tab, so each deposit runs with the main tab selected and the player's tab selection in the bank is restored afterward. */
private inline fun ProtectedAccess.intoMainTab(block: () -> Unit) {
    val previous = selectedTab
    if (previous != BankTab.Main) {
        selectedTab = BankTab.Main
    }
    try {
        block()
    } finally {
        if (previous != BankTab.Main) {
            selectedTab = previous
        }
    }
}

internal fun ProtectedAccess.bankDeposit(
    bankInv: BankInvScript,
    slot: Int,
    count: Int,
    from: Inventory,
): Boolean {
    var deposited = false
    intoMainTab { deposited = with(bankInv) { this@bankDeposit.invDeposit(slot, count, from) } }
    return deposited
}

/** Deposits the whole player inventory and returns whether anything was actually deposited.
 * Note: I utilize the functions straight from the bank scripts. */
internal fun ProtectedAccess.bankDepositInv(bankInv: BankInvScript): Boolean {
    val freeBefore = inv.freeSpace()
    intoMainTab { with(bankInv) { this@bankDepositInv.depositInv() } }
    return inv.freeSpace() != freeBefore
}

internal fun ProtectedAccess.bankDepositWorn(bankInv: BankInvScript): Boolean {
    val freeBefore = worn.freeSpace()
    intoMainTab { with(bankInv) { this@bankDepositWorn.depositWorn() } }
    return worn.freeSpace() != freeBefore
}

/** Deposits count of the item at clickedSlot using the deposit-ordering config, playing the animation whenever anything is deposited. */
internal fun ProtectedAccess.depositInventoryItem(
    bankInv: BankInvScript,
    clickedSlot: Int,
    count: Int,
) {
    if (inv[clickedSlot] == null || count <= 0) {
        return
    }
    if (bankDeposit(bankInv, resolveDepositSlot(clickedSlot), count, inv)) {
        playDepositAnim()
    }
}
