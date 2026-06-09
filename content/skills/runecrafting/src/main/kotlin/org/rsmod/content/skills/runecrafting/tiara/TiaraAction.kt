package org.rsmod.content.skills.runecrafting.tiara

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.runecrafting.RunecraftingTiaraRow
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.advanceRunecraftingXp

object TiaraAction {
    fun ProtectedAccess.createTiara(
        talismanId: String,
        def: RunecraftingTiaraRow,
        xpMods: XpModifiers,
    ) {
        val tiaraItem = "obj.tiara"
        if (!inv.contains(tiaraItem)) {
            mes("You need a tiara to bind to your talisman.")
            return
        }

        if (!inv.contains(talismanId)) {
            mes("You need a talisman to bind a tiara here.")
            return
        }

        val removedTiara = invDel(inv, tiaraItem, 1).success
        val removedTalisman = invDel(inv, talismanId, 1).success

        if (removedTiara && removedTalisman) {
            if (invAdd(inv, def.item.internalName, 1).success) {
                advanceRunecraftingXp(def.xp.toDouble(), xpMods)
                mes("You bind the power of the talisman into your tiara.")
            }
        }
    }

    fun ProtectedAccess.createSpecialTiara(
        talismanId: String,
        outputTiara: String,
        xp: Int,
        xpMods: XpModifiers,
    ) {
        if (!inv.contains("obj.tiara_gold")) {
            mes("You need a gold tiara to bind to your talisman.")
            return
        }

        if (!inv.contains(talismanId)) {
            mes("You need a talisman to bind a tiara here.")
            return
        }

        if (invDel(inv, "obj.tiara_gold", 1).failure) {
            return
        }
        if (invDel(inv, talismanId, 1).failure) {
            return
        }

        if (invAdd(inv, outputTiara, 1).success) {
            advanceRunecraftingXp(xp.toDouble(), xpMods)
            mes("You bind the power of the talisman into your tiara.")
        }
    }
}
