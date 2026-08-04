package org.rsmod.content.interfaces.bank

import org.rsmod.api.player.ironman.IronmanRestrictions
import org.rsmod.api.player.protect.ProtectedAccess

fun ProtectedAccess.tryOpenBank(): Boolean {
    if (IronmanRestrictions.blockUimBank(player)) {
        return false
    }
    ifOpenMainSidePair(main = "interface.bankmain", side = "interface.bankside")
    return true
}
