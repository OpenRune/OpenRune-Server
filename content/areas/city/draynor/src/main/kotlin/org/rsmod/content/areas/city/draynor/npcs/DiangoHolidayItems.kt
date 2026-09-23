package org.rsmod.content.areas.city.draynor.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpNpc4
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private const val MASK_VARPS = 8

private fun ProtectedAccess.holidayMasks(): List<Int> =
    (1..MASK_VARPS).map { player.vars["varp.diango_holiday_items_$it"] }

/**
 * Diango's Holiday Item Retrieval. Each `holiday_items` enum index is one bit across the eight
 * `diango_holiday_items_*` varps; holiday events set a bit when they award the item, and Diango
 * hands back any unlocked item the player no longer carries.
 */
class DiangoHolidayItems : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc4(DIANGO) { openHolidayItems() }
        onIfModalButton("component.rareitems_diango:items") { button ->
            when (button.op) {
                IfButtonOp.Op1 -> takeItem(button.comsub)
                IfButtonOp.Op10 -> button.obj?.let { mes(it.examine) }
                else -> Unit
            }
        }
        onIfModalButton("component.rareitems_diango:com_2") { ifClose() }
    }

    private fun ProtectedAccess.takeItem(index: Int) {
        if (!isUnlocked(index)) return
        val obj = holidayItem(index) ?: return
        val name = obj.internalName
        if (inv.count(name) > 0 || bank.count(name) > 0) {
            mes("You already have one of those.")
            return
        }
        if (inv.isFull()) {
            mes("You don't have enough inventory space.")
            return
        }
        invAdd(inv, name)
        runClientScript(UPDATE_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), *holidayMasks().toTypedArray())
    }

    private fun ProtectedAccess.isUnlocked(index: Int): Boolean {
        val mask = holidayMasks().getOrNull(index / BITS_PER_VARP) ?: return false
        return mask and (1 shl (index % BITS_PER_VARP)) != 0
    }

    private fun holidayItem(index: Int) =
        ServerCacheManager.getEnum(HOLIDAY_ITEMS.asRSCM(RSCMType.ENUM))
            ?.values
            ?.get(index)
            ?.let { ServerCacheManager.getItem((it as Number).toInt()) }

    companion object {
        private const val DIANGO = "npc.aprilfoolshorsesalesman"
        private const val HOLIDAY_ITEMS = "enum.holiday_items"
        private const val INIT_SCRIPT = "clientscript.[clientscript,rareitems_diango]"
        private const val UPDATE_SCRIPT = "clientscript.[clientscript,rareitems_diango_update]"
        private const val TITLE = "Diango's Holiday Item Retrieval"
        private const val BITS_PER_VARP = 32
        private const val ITEM_SLOTS = 227

        fun ProtectedAccess.openHolidayItems() {
            ifOpenMainModal("interface.rareitems_diango")
            runClientScript(INIT_SCRIPT.asRSCM(RSCMType.CLIENTSCRIPT), TITLE, *holidayMasks().toTypedArray())
            ifSetEvents("component.rareitems_diango:items", 0..ITEM_SLOTS, IfEvent.Op1, IfEvent.Op10)
        }
    }
}
