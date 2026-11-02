package org.rsmod.content.skills.runecrafting.tiara

import org.rsmod.api.player.events.skilling.SkillingInput
import org.rsmod.api.player.events.skilling.SkillingProductSource
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.stats.xpmod.XpModifiers
import org.rsmod.api.table.runecrafting.RunecraftingTiaraRow
import org.rsmod.content.skills.runecrafting.action.RunecraftAction.awardRunecraftProduct

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

        bindTiara(tiaraItem, talismanId, def.item.internalName, def.xp, xpMods)
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

        bindTiara("obj.tiara_gold", talismanId, outputTiara, xp, xpMods)
    }

    private fun ProtectedAccess.bindTiara(
        tiara: String,
        talisman: String,
        output: String,
        xp: Int,
        xpMods: XpModifiers,
    ) {
        val bound =
            awardRunecraftProduct(
                output,
                1,
                xp.toDouble(),
                xpMods,
                SkillingProductSource.TiaraBinding,
                listOf(SkillingInput(tiara), SkillingInput(talisman)),
            )
        if (bound) {
            mes("You bind the power of the talisman into your tiara.")
        }
    }
}
