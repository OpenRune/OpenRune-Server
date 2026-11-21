package org.rsmod.content.interfaces.bank

import dev.openrune.ServerCacheManager
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.type.getInvObj
import org.rsmod.game.type.placeholder

private var Player.bankMainTabSize by intVarBit("varbit.bank_tab_main")

/**
 * Moves the obj in [fromSlot] of [from] into the player's bank without needing protected access
 * to the bank interface. Stacks onto an existing entry or placeholder, otherwise takes the next
 * free slot at the end of the main tab. The move is one transaction; on `false` nothing moved.
 */
fun Player.bankAppend(from: Inventory, fromSlot: Int): Boolean {
    val obj = from[fromSlot] ?: return false
    val bank = invMap.getOrPut("inv.bank")
    val capacity = bankCapacity
    val placeholder = placeholder(getInvObj(obj))

    val existing = bank.indexOfFirst { it?.id == obj.id || it?.id == placeholder.id }
    val appendsToMain = existing == -1
    val slot =
        if (!appendsToMain) {
            existing
        } else {
            val mainFirst = BankTab.tabs.sumOf { vars[it.sizeVarBit] }
            val end = mainFirst + bankMainTabSize
            if (end >= capacity || bank[end] != null) {
                return false
            }
            end
        }

    val result =
        invTransaction(from, bank) {
            val fromInv = select(from)
            val bankInv = select(bank)
            transfer {
                this.from = fromInv
                this.into = bankInv
                this.fromSlot = fromSlot
                this.intoSlot = slot
                this.intoCapacity = capacity
                this.count = obj.count
                this.strict = true
            }
        }
    if (!result.success) {
        return false
    }

    if (appendsToMain) {
        bankMainTabSize += 1
    }
    return true
}

/**
 * Puts [count] of [obj] (unnoted) straight into the player's bank: onto an existing stack,
 * otherwise at the end of the main tab. A placeholder is left for the caller to handle, so this
 * returns `false` then. On `false` the bank is unchanged.
 */
fun Player.bankInsert(obj: Int, count: Int): Boolean {
    if (count <= 0) {
        return false
    }
    val type = ServerCacheManager.getItem(obj) ?: return false
    val base = if (type.isCert) ServerCacheManager.getItem(type.certlink) ?: return false else type
    val bank = invMap.getOrPut("inv.bank")
    val capacity = bankCapacity
    val placeholder = placeholder(base)

    val existing = bank.indexOfFirst { it?.id == base.id || it?.id == placeholder.id }
    if (existing != -1 && bank[existing]?.id != base.id) {
        return false
    }
    val appendsToMain = existing == -1
    val slot =
        if (!appendsToMain) {
            existing
        } else {
            val mainFirst = BankTab.tabs.sumOf { vars[it.sizeVarBit] }
            val end = mainFirst + bankMainTabSize
            if (end >= capacity || bank[end] != null) {
                return false
            }
            end
        }

    val result =
        invTransaction(bank) {
            val bankInv = select(bank)
            insert {
                this.into = bankInv
                this.obj = base.id
                this.strictCount = count
                this.strictSlot = slot
            }
        }
    if (!result.success) {
        return false
    }

    if (appendsToMain) {
        bankMainTabSize += 1
    }
    return true
}
