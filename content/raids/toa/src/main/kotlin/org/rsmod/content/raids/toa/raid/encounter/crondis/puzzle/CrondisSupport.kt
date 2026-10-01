package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

internal const val CONTAINER_FULL = 100

private val CONTAINER_ID: Int by lazy { CrondisObjs.CONTAINER.asRSCM(RSCMType.OBJ) }

internal fun Player.containerSlot(): Int? = inv.indices.firstOrNull { inv[it]?.id == CONTAINER_ID }

internal fun Player.containerWater(): Int {
    val slot = containerSlot() ?: return 0
    return inv[slot]?.vars ?: 0
}

internal fun Player.setContainerWater(slot: Int, water: Int) {
    val obj = inv[slot] ?: return
    inv[slot] = InvObj(CrondisObjs.CONTAINER, obj.count, vars = water)
}

internal fun Player.removeContainers() {
    for (slot in inv.indices) {
        if (inv[slot]?.id == CONTAINER_ID) inv[slot] = null
    }
}
