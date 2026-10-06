package org.rsmod.content.minigames.gauntlet

import dev.openrune.util.Wearpos
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.player.ui.PlayerInterfaceUpdates
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.game.entity.Player

internal var Player.holdingInventory by boolVarBit("varbit.holding_inventory_location")

internal object GauntletHolding {
    private const val HOLDING_INV = "inv.gauntlet_holding"
    private const val INV_SIZE = 28
    private const val WORN_OFFSET = INV_SIZE

    fun store(player: Player) {
        val holding = player.invMap.getOrPut(HOLDING_INV)
        check(holding.size >= WORN_OFFSET + player.worn.size) { "Holding inv is too small." }
        if (player.holdingInventory) return
        for (slot in 0 until player.inv.size) {
            holding[slot] = player.inv[slot]
        }
        for (slot in 0 until player.worn.size) {
            holding[WORN_OFFSET + slot] = player.worn[slot]
        }
        player.inv.fillNulls()
        player.worn.fillNulls()
        player.holdingInventory = true
        refreshEquipment(player)
    }

    fun restore(player: Player) {
        if (!player.holdingInventory) return
        val holding = player.invMap.getOrPut(HOLDING_INV)
        player.inv.fillNulls()
        player.worn.fillNulls()
        for (slot in 0 until player.inv.size) {
            player.inv[slot] = holding[slot]
        }
        for (slot in 0 until player.worn.size) {
            player.worn[slot] = holding[WORN_OFFSET + slot]
        }
        holding.fillNulls()
        player.holdingInventory = false
        refreshEquipment(player)
    }

    fun giveStartingKit(player: Player, mode: GauntletMode) {
        val suffix = if (mode.corrupted) "_hm" else ""
        for (tool in listOf("axe", "pickaxe", "harpoon")) {
            player.invAdd(player.inv, "obj.gauntlet_$tool$suffix", 1)
        }
        player.invAdd(player.inv, "obj.gauntlet_pestle", 1)
        player.invAdd(player.inv, "obj.gauntlet_teleport_crystal$suffix", 1)
        player.invAdd(
            player.worn,
            "obj.gauntlet_sceptre$suffix",
            1,
            slot = Wearpos.RightHand.slot,
        )
        refreshEquipment(player)
    }

    private fun refreshEquipment(player: Player) {
        player.rebuildAppearance()
        PlayerInterfaceUpdates.updateCombatLevel(player)
        PlayerInterfaceUpdates.updateCombatTab(player)
    }
}
