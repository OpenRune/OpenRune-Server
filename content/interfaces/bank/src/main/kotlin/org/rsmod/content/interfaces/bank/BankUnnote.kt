package org.rsmod.content.interfaces.bank

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType
import kotlin.math.min
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.game.inv.isType

suspend fun Dialogue.confirmAndExchangeBanknote(invSlot: Int, objType: ItemServerType) {
    val confirmation = choice2("Yes", true, "No", false, "Un-note the banknote?")
    if (!confirmation) {
        return
    }

    val invObj = access.inv[invSlot]
    check(invObj.isType(objType)) {
        "Unexpected `invObj` when un-certifying! (found=$invObj, expectedType=$objType)"
    }

    val count = min(access.inv.freeSpace(), invObj.count)
    if (count == 0) {
        access.mes("You don't have any inventory space.")
        return
    }

    val uncert = access.ocUncert(objType)
    val replace = access.invReplace(access.inv, invSlot, count, uncert)
    if (replace.success) {
        objbox(
            RSCM.getReverseMapping(RSCMType.OBJ, uncert.id),
            400,
            "The bank exchanges your banknote for an item.",
        )
    }
}
