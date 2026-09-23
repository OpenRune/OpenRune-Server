package org.rsmod.content.drops

import org.rsmod.game.entity.Player

private const val INV = "inv.inv"
private const val BANK = "inv.bank"

public fun Player.hasObjInInventoryOrBank(obj: String): Boolean {
    val inv = invMap[INV]
    if (inv != null && obj in inv) {
        return true
    }
    val bank = invMap[BANK]
    return bank != null && obj in bank
}
