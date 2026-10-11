package org.rsmod.content.interfaces.bank

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.Inventory

/** Optional specialised bank stores own their state after the physical transfer commits. */
interface BankDepositStorage {
    fun accepts(access: ProtectedAccess, inventory: Inventory, slot: Int): Boolean
    fun deposit(access: ProtectedAccess, inventory: Inventory, slot: Int, count: Int): Int
}
