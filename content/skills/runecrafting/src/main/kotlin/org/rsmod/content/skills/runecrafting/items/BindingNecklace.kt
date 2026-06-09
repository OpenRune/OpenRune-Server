package org.rsmod.content.skills.runecrafting.items

import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.skills.runecrafting.bindingNecklaceCharges
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

object BindingNecklace {
    const val ITEM = "obj.magic_emerald_necklace"
    const val MAX_CHARGES = 16

    fun Player.isWearing(): Boolean = ITEM in worn

    fun Player.prepareChargesForUse() {
        if (bindingNecklaceCharges <= 0) {
            bindingNecklaceCharges = MAX_CHARGES
        }
    }

    fun ProtectedAccess.consumeChargeAfterCombo(): Boolean {
        if (!player.isWearing()) {
            return false
        }

        player.prepareChargesForUse()
        player.bindingNecklaceCharges--

        if (player.bindingNecklaceCharges <= 0) {
            disintegrateWornNecklace()
            return true
        }

        return true
    }

    private fun ProtectedAccess.disintegrateWornNecklace() {
        val slot = player.worn.indexOfFirst { obj -> obj != null && obj.isType(ITEM) }
        if (slot >= 0) {
            invDel(worn, ITEM, 1, slot = slot)
        }
        mes("Your binding necklace has disintegrated.")
    }
}
