package org.alter.game.model.entity

import org.alter.game.model.inv.Inventory
import kotlin.collections.plusAssign
import kotlin.text.clear
import kotlin.text.get

object PlayerInvUpdateProcessor {
    private val processedInvs = hashSetOf<Inventory>()

    public fun process(player: Player) {
        player.updateTransmittedInvs()
        player.processQueuedTransmissions()
    }

    public fun cleanUp() {
        processedInvs.forEach(Inventory::clearModifiedSlots)
        processedInvs.clear()
    }

    private fun Player.updateTransmittedInvs() {
        for (transmitted in transmittedInvs.iterator()) {
            val inv = invMap.backing[transmitted]
            checkNotNull(inv) { "Inv expected in `invMap`: $transmitted (invMap=${invMap})" }
            if (!inv.hasModifiedSlots()) {
                continue
            }
            UpdateInventory.updateInvPartial(this, inv)
            updatePendingRunWeight(inv)
            processedInvs += inv
        }
    }

    private fun Player.processQueuedTransmissions() {
        for (add in transmittedInvAddQueue.iterator()) {
            val inv = invMap.backing[add]
            checkNotNull(inv) { "Inv expected in `invMap`: $add (invMap=${invMap})" }
            UpdateInventory.updateInvFull(this, inv)
            updatePendingRunWeight(inv)
            transmittedInvs.add(add)
            processedInvs += inv
        }
        transmittedInvAddQueue.clear()
    }

    private fun Player.updatePendingRunWeight(inventory: Inventory) {
        val updateRunWeight = inventory.type.runWeight
        if (updateRunWeight) {
            pendingRunWeight = true
        }
    }

}
