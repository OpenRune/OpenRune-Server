package org.rsmod.content.minigames.gauntlet

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.game.entity.Player

internal fun Player.exchangeGauntlet(
    inputs: List<Pair<String, Int>>,
    product: String,
    count: Int = 1,
): Boolean = invTransaction(inv) {
    val inventory = select(inv)
    for ((obj, amount) in inputs) delete(inventory, obj.asRSCM(RSCMType.OBJ), amount)
    add(inventory, product.asRSCM(RSCMType.OBJ), count)
}.success
