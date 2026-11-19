package org.rsmod.content.areas.misc.tutorialisland

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess

internal fun ProtectedAccess.giveTutorialItems(vararg items: Pair<String, Int>): Boolean {
    if (items.isEmpty()) {
        return true
    }
    val transaction =
        player.invTransaction(inv, autoCommit = false) {
            val inventory = select(inv)
            for ((item, count) in items) {
                add(inventory, item.asRSCM(RSCMType.OBJ), count)
            }
        }
    if (transaction.failure) {
        return false
    }
    transaction.commitAll()
    return true
}
