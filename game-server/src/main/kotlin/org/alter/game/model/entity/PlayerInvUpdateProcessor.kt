package org.alter.game.model.entity

import org.alter.game.model.inv.Inventory
import kotlin.collections.plusAssign

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
        transmittedInvs.forEach { transmitted ->
            val inv = inventory
            checkNotNull(inv) { "Inv expected in `invMap`: $transmitted (invMap=${invMap})" }
            if (!inv.hasModifiedSlots()) {
                return
            }

            UpdateInventory.updateInvPartial(this, inv)
            updatePendingRunWeight(inv)
            if (transmitted == "inv.worn") {
                calculateBonuses()
            }
            processedInvs += inv
        }
    }

    private fun Player.processQueuedTransmissions() {
        transmittedInvAddQueue.forEach { add ->
            UpdateInventory.updateInvFull(this, inventory)
            updatePendingRunWeight(inventory)
            if (add == "inv.worn") {
                calculateBonuses()
            }
            transmittedInvs.add(add)
            processedInvs += inventory
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
