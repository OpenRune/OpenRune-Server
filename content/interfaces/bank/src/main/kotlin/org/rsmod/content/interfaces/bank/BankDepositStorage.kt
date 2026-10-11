package org.rsmod.content.interfaces.bank

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.game.inv.Inventory

/** Optional specialised bank stores own their state after the physical transfer commits. */
interface BankDepositStorage {
    fun accepts(access: ProtectedAccess, inventory: Inventory, slot: Int): Boolean = false
    fun deposit(access: ProtectedAccess, inventory: Inventory, slot: Int, count: Int): Int = 0

    /** No suspension: each owner removes only the contents its native bank transfer committed. */
    fun depositContainers(access: ProtectedAccess, bank: BankInvScript): Long = 0L
}
