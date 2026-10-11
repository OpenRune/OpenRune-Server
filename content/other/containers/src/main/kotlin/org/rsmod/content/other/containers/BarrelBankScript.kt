package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.script.*
import org.rsmod.content.interfaces.bank.*
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class BarrelBankScript @Inject constructor(private val barrel: ChuggingBarrel, private val loadouts: BarrelLoadouts) : PluginScript() {
    override fun ScriptContext.startup() {
        onProtectedEvent<HeldBanksideEvents.Configure>(ChuggingBarrel.ITEM.asRSCM()) { if (inv[it.slot]?.id == ChuggingBarrel.ITEM.asRSCM() && valid(false)) show() }
        onIfClose("interface.bankside") { if (player.attr[PANEL] == true) { player.attr.remove(PANEL); player.invMap[ChuggingBarrel.DISPLAY]?.let { player.stopInvTransmit(it) } } }
        onIfModalButton(PREFIX + "dismiss") { player.attr.remove(PANEL); ifSetHide(PREFIX + "container", true); player.invMap[ChuggingBarrel.DISPLAY]?.let { player.stopInvTransmit(it) } }
        onIfModalButton(PREFIX + "bankall") { if (valid()) { with(loadouts) { for (slot in 0..4) deposit(slot, Int.MAX_VALUE) }; refresh() } }
        onIfModalButton(PREFIX + "fill") { if (valid()) { with(loadouts) { for (slot in 0..4) fill(slot, Int.MAX_VALUE) }; refresh() } }
        onIfModalButton(CONTENTS) { contentOp(it) }
        onIfModalDrag(CONTENTS) { val from = it.selectedSlot; val to = it.targetSlot; val items = barrel.contents(player)
            if (valid() && from in 0..4 && to in 0..4 && it.selectedObj == items[from!!]?.id && it.targetObj == items[to!!]?.id) { barrel.swap(player, from, to); refresh() }
        }
        for (index in 0..3) {
            val prefix = PREFIX + "loadout_${index}_"
            onIfModalButton(prefix + "save") { if (valid() && it.op == IfButtonOp.Op1) { BarrelLoadoutCodec.write(player, index, barrel.contents(player)); refresh() } }
            onIfModalButton(prefix + "load") { if (valid() && it.op == IfButtonOp.Op1) { with(loadouts) { load(index) }; refresh() } }
            onIfModalButton(prefix + "contents") { presetOp(index, it) }
            onIfModalDrag(prefix + "contents") {
                val from = it.selectedSlot; val to = it.targetSlot; val items = BarrelLoadoutCodec.read(player, index).toMutableList()
                if (valid() && from in 0..4 && to in 0..4 && it.selectedObj == items[from!!]?.id && it.targetObj == items[to!!]?.id) { val old = items[from]; items[from] = items[to]; items[to] = old; BarrelLoadoutCodec.write(player, index, items); refresh() }
            }
            for (source in listOf("component.bankside:items", "component.bankmain:items", CONTENTS)) onIfModalDrag(source, prefix + "contents") {
                val slot = it.selectedSlot ?: return@onIfModalDrag; val target = it.targetSlot ?: return@onIfModalDrag
                val id = when (source) { CONTENTS -> barrel.contents(player).getOrNull(slot)?.id; "component.bankside:items" -> inv[slot]?.id; else -> bank[slot]?.id }
                val family = id?.let(PotionFamilies::byItem)
                if (valid() && target in 0..4 && family != null && it.selectedObj == id) { val items = BarrelLoadoutCodec.read(player, index).toMutableList(); for (i in 0..4) if (items[i]?.id == family.id) items[i] = null; items[target] = ChuggingBarrel.Slot(family.id, 100); BarrelLoadoutCodec.write(player, index, items); refresh() }
            }
        }
    }
    private fun ProtectedAccess.valid(panel: Boolean = true) = player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut && barrel.owns(player) && player.ui.containsModal("interface.bankmain") && (!panel || player.attr[PANEL] == true)
    private fun ProtectedAccess.show() {
        player.attr[PANEL] = true; ifSetHide(PREFIX + "container", false); player.startInvTransmit(player.invMap.getOrPut(ChuggingBarrel.DISPLAY))
        runClientScript(7353, ChuggingBarrel.ITEM.asRSCM())
        for (name in listOf("dismiss", "bankall", "fill")) ifSetEvents(PREFIX + name, -1..-1, IfEvent.Op1)
        ifSetEvents(CONTENTS, 0..4, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op6, IfEvent.Op7, IfEvent.Op8, IfEvent.Op9, IfEvent.Op10, IfEvent.DragTarget)
        for (index in 0..3) { val prefix = PREFIX + "loadout_${index}_"; for (name in listOf("load", "save")) ifSetEvents(prefix + name, -1..-1, IfEvent.Op1); ifSetEvents(prefix + "contents", 0..4, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op10, IfEvent.DragTarget) }
        refresh()
    }
    private fun ProtectedAccess.refresh() { val display = player.invMap.getOrPut(ChuggingBarrel.DISPLAY); display.fillNulls(); for ((slot, item) in barrel.contents(player).withIndex()) if (item != null) display[slot] = InvObj(ServerCacheManager.getItem(item.id)!!, item.count); invTransmit(display) }
    private suspend fun ProtectedAccess.contentOp(event: IfModalButton) {
        if (!valid() || event.comsub !in 0..4) return
        val item = barrel.contents(player)[event.comsub] ?: return
        if (event.obj?.id?.let { it != item.id } == true) return
        if (event.op == IfButtonOp.Op10) { mes(ServerCacheManager.getItem(item.id)!!.examine); return }
        val before = barrel.contents(player); val origin = coords
        val requested = when (event.op) {
            IfButtonOp.Op1 -> when (leftClickQtyMode) { QuantityMode.One -> 1; QuantityMode.Five -> 5; QuantityMode.Ten -> 10; QuantityMode.X -> lastQtyInput; QuantityMode.All -> Int.MAX_VALUE }
            IfButtonOp.Op2 -> 1; IfButtonOp.Op3 -> 5; IfButtonOp.Op4 -> 10; IfButtonOp.Op5 -> lastQtyInput; IfButtonOp.Op6, IfButtonOp.Op8 -> countDialog(); IfButtonOp.Op7, IfButtonOp.Op9 -> Int.MAX_VALUE; else -> return
        }
        ifCloseChat(); if (!valid() || coords != origin || before != barrel.contents(player)) return
        with(loadouts) { if (event.op == IfButtonOp.Op8 || event.op == IfButtonOp.Op9) fill(event.comsub, requested) else deposit(event.comsub, requested) }; refresh()
    }
    private suspend fun ProtectedAccess.presetOp(index: Int, event: IfModalButton) {
        if (!valid() || event.comsub !in 0..4) return
        val items = BarrelLoadoutCodec.read(player, index).toMutableList(); val item = items[event.comsub]
        if (event.obj?.id?.let { it != item?.id } == true) return
        val origin = coords
        val barrelSlot = inv.indexOfFirst { it?.id == ChuggingBarrel.ITEM.asRSCM() }
        val heldBarrel = inv[barrelSlot]
        when (event.op) {
            IfButtonOp.Op1 -> {
                val candidates = PotionFamilies.all.filter { family -> family.doses.filterKeys { it > 0 }.values.any { id -> inv.any { it?.id == id } || bank.any { it?.id == id } } || barrel.contents(player).any { it?.id == family.id } }.sortedBy { ServerCacheManager.getItem(it.id)!!.name }
                if (candidates.isEmpty()) { mes("You have no suitable potions in your bank or inventory."); return }
                var page = 0; var selected: PotionFamilies.Family? = null
                while (selected == null) {
                    val choices = candidates.drop(page * 5).take(5)
                    val picked = menu("Choose potion", hotkeys = true, choices = choices.map { ServerCacheManager.getItem(it.id)!!.name } + listOf("Next page", "Cancel"))
                    if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || coords != origin || inv[barrelSlot] !== heldBarrel || BarrelLoadoutCodec.read(player, index) != items) return
                    ifOpenMainSidePair("interface.bankmain", "interface.bankside"); show()
                    if (picked in choices.indices) selected = choices[picked] else if (picked == choices.size) page = if ((page + 1) * 5 < candidates.size) page + 1 else 0 else return
                }
                for (i in 0..4) if (items[i]?.id == selected.id) items[i] = null
                items[event.comsub] = ChuggingBarrel.Slot(selected.id, item?.count ?: 100)
            }
            IfButtonOp.Op2 -> items[event.comsub] = null
            IfButtonOp.Op3 -> { if (item == null) return; val count = countDialog(); if (!valid() || coords != origin || BarrelLoadoutCodec.read(player, index) != items || count !in 1..100) return; items[event.comsub] = item.copy(count = count) }
            IfButtonOp.Op10 -> { item?.let { mes(ServerCacheManager.getItem(it.id)!!.examine) }; return }
            else -> return
        }
        ifCloseChat(); if (valid() && coords == origin) { BarrelLoadoutCodec.write(player, index, items); refresh() }
    }
    companion object { private const val PREFIX = "component.bankside:prepot_device_"; const val CONTENTS = PREFIX + "contents"; private val PANEL = AttributeKey<Boolean>() }
}
