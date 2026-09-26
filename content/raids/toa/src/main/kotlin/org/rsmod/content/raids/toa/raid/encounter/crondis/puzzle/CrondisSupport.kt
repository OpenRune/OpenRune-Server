package org.rsmod.content.raids.toa.raid.encounter.crondis.puzzle

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj

/** A full container. Its water is kept in the obj's vars, as a percentage. */
internal const val CONTAINER_FULL = 100

private val CONTAINER_ID: Int by lazy { CrondisObjs.CONTAINER.asRSCM(RSCMType.OBJ) }

/** Slot of this player's water container, or `null`. */
internal fun Player.containerSlot(): Int? = inv.indices.firstOrNull { inv[it]?.id == CONTAINER_ID }

/** Water in this player's container; 0 without one. */
internal fun Player.containerWater(): Int {
    val slot = containerSlot() ?: return 0
    return inv[slot]?.vars ?: 0
}

/** Built by name: InvObj's Int constructor is @UncheckedType (opt-in only). */
internal fun Player.setContainerWater(slot: Int, water: Int) {
    val obj = inv[slot] ?: return
    inv[slot] = InvObj(CrondisObjs.CONTAINER, obj.count, vars = water)
}

/** Takes every water container away (end of room, reset, leaving). */
internal fun Player.removeContainers() {
    for (slot in inv.indices) {
        if (inv[slot]?.id == CONTAINER_ID) inv[slot] = null
    }
}
