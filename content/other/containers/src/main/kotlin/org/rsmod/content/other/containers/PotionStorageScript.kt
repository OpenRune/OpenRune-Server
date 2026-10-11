package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.*
import org.rsmod.content.interfaces.bank.*
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class PotionStorageScript @Inject constructor(private val storage: PotionStorage) : PluginScript() {
    override fun ScriptContext.startup() {
        onIfOpen("interface.bankside") {
            for (slot in 1..8) if (player.vars["varp.potionstore_favourite_$slot"] == 0)VarPlayerIntMapSetter.set(player, "varp.potionstore_favourite_$slot", -1)
            for (button in listOf("potionstore_button", "depositpotion_toggle", "chuggingbarrel_toggle"))player.ifSetEvents("component.bankmain:$button", -1..-1, IfEvent.Op1)
            player.ifSetEvents(ITEMS, 0..599, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op6, IfEvent.Op7, IfEvent.Op8, IfEvent.Op9, IfEvent.Op10)
        }
        onIfModalButton("component.bankmain:potionstore_button") {
            if (!valid(false) || it.op != IfButtonOp.Op1) return@onIfModalButton
            val before = player.vars["varbit.bank_currenttab"]
            if (before == 15) VarPlayerIntMapSetter.set(player, "varbit.bank_currenttab", player.vars["varbit.potionstore_previous_tab"].coerceIn(0, 9))
            else { VarPlayerIntMapSetter.set(player, "varbit.potionstore_previous_tab", before.coerceIn(0, 9)); VarPlayerIntMapSetter.set(player, "varbit.bank_currenttab", 15) }
            runClientScript(6079)
        }
        for ((button, bit) in listOf("depositpotion_toggle" to "varbit.bank_depositpotion", "chuggingbarrel_toggle" to "varbit.bank_chugging_fill_open")) onIfModalButton("component.bankmain:$button") {
            if (valid(false) && it.op == IfButtonOp.Op1)VarPlayerIntMapSetter.set(player, bit, 1 - player.vars[bit])
        }
        onIfModalButton(ITEMS) { operate(it) }
    }
    private fun ProtectedAccess.valid(open: Boolean = true) = storage.unlocked(player) && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && player.ui.containsModal("interface.bankmain") && (!open || player.vars["varbit.bank_currenttab"] == 15)
    private suspend fun ProtectedAccess.operate(event: IfModalButton) {
        if (!valid() || event.comsub < 0) return
        val family = PotionStorageCatalog.all.getOrNull(event.comsub / 5)
        val vial = event.comsub in storage.vialSlots(player)
        if (family == null && !vial) return
        if (!vial) {
            family!!
            val selected = family.doses.getValue(storage.size(player, family))
            if (event.obj?.id?.let { it != selected } == true) return
            if (storage.count(player, family) == 0 && family.doses[1] !in storage.favourites(player)) return
            when (event.comsub % 5) {
                1 -> {
                    val dose = when (event.op) { IfButtonOp.Op6 -> 1; IfButtonOp.Op7 -> 2; IfButtonOp.Op8 -> 3; IfButtonOp.Op9 -> 4; IfButtonOp.Op10 -> { mes(ServerCacheManager.getItem(selected)!!.examine); return }; else -> return }
                    if (dose <= family.maximum)storage.setSize(player, family, dose); return
                }
                4 -> { if (event.op == IfButtonOp.Op1 && !storage.favourite(player, family))mes("You can save up to eight favourite potions."); return }
                0 -> Unit
                else -> return
            }
        }
        val origin = coords; val before = family?.let { storage.count(player, it) }; val beforeVials = storage.vials(player); val beforeSize = family?.let { storage.size(player, it) }
        val bottleCount = if (vial)beforeVials else ((before!!.toLong() + storage.size(player, family!!) - 1) / storage.size(player, family)).toInt()
        val count = when (event.op) {
            IfButtonOp.Op1 -> when (leftClickQtyMode) { QuantityMode.One -> 1; QuantityMode.Five -> 5; QuantityMode.Ten -> 10; QuantityMode.X -> lastQtyInput; QuantityMode.All -> Int.MAX_VALUE }
            IfButtonOp.Op2 -> 1; IfButtonOp.Op3 -> 5; IfButtonOp.Op4 -> 10; IfButtonOp.Op5 -> lastQtyInput
            IfButtonOp.Op6 -> countDialog(); IfButtonOp.Op7 -> Int.MAX_VALUE; IfButtonOp.Op8 -> (bottleCount - 1).coerceAtLeast(0); else -> return
        }
        ifCloseChat()
        if (!valid() || coords != origin || beforeVials != storage.vials(player) || family?.let { before != storage.count(player, it) || beforeSize != storage.size(player, it) } == true) return
        with(storage) { if (vial)withdrawVials(count, withdrawCert) else withdraw(family!!, count, withdrawCert) }
    }
    companion object { const val ITEMS = "component.bankmain:potionstore_items" }
}
