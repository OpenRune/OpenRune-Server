package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.player.stopInvTransmit
import org.rsmod.api.player.ui.IfModalButton
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.content.other.consumables.ConsumableDelayState
import org.rsmod.content.other.consumables.ConsumableType
import org.rsmod.content.other.consumables.potion.PotionEffectService
import org.rsmod.game.inv.InvObj
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class ChuggingBarrelScript @Inject constructor(private val barrel: ChuggingBarrel, private val banks: ContainerBankProximity, private val effects: PotionEffectService) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(ChuggingBarrel.DISASSEMBLED) { assemble(it.slot) }
        onOpHeld1(ChuggingBarrel.ITEM) { if (valid()) drink() }
        onOpHeld2(ChuggingBarrel.ITEM) { if (valid()) open() }
        onOpHeld3(ChuggingBarrel.ITEM) { dismantle(it.slot) }
        onOpHeldU(ChuggingBarrel.ITEM) {
            if (!valid() || inv[it.firstSlot]?.id != ChuggingBarrel.ITEM.asRSCM() || inv[it.secondSlot]?.id != it.second.id) return@onOpHeldU
            val moved = with(barrel) { store(it.secondSlot, Int.MAX_VALUE) }
            mes(if (moved > 0) "Your chugging barrel has been filled with $moved doses." else "Your barrel cannot store that potion or is full.")
        }
        onProtectedEvent<HeldBanksideEvents.Operate>(ChuggingBarrel.ITEM.asRSCM()) { if (inv[it.slot]?.id == ChuggingBarrel.ITEM.asRSCM() && valid()) drink() }
        onIfOpen(MAIN) { player.startInvTransmit(player.invMap.getOrPut(ChuggingBarrel.DISPLAY)) }
        onIfClose(MAIN) { player.invMap[ChuggingBarrel.DISPLAY]?.let { player.stopInvTransmit(it); it.fillNulls() } }
        onDestroyHeld { if (type.asRSCM() == ChuggingBarrel.ITEM.asRSCM()) barrel.clear(player) }
        onIfModalButton(CONTENTS) { contentOp(it) }
        onIfModalButton(INVENTORY) { inventoryOp(it) }
        onIfModalDrag(CONTENTS) {
            if (!valid()) return@onIfModalDrag
            val from = it.selectedSlot ?: return@onIfModalDrag; val to = it.targetSlot ?: return@onIfModalDrag
            val items = barrel.contents(player)
            if (from !in 0..4 || to !in 0..4 || it.selectedObj != items[from]?.id || it.targetObj != items[to]?.id) return@onIfModalDrag
            barrel.swap(player, from, to); refresh()
        }
        onIfModalDrag(INVENTORY, CONTENTS) {
            val slot = it.selectedSlot ?: return@onIfModalDrag
            if (valid() && slot in inv.indices && inv[slot]?.id == it.selectedObj) { with(barrel) { store(slot, Int.MAX_VALUE) }; refresh() }
        }
    }
    private fun ProtectedAccess.valid(): Boolean {
        if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || !barrel.owns(player)) return false
        if (!banks.available(player)) { mes("You must be in a bank or next to a bank chest to use the barrel."); return false }
        return true
    }
    private suspend fun ProtectedAccess.assemble(slot: Int) {
        val item = inv[slot]?.takeIf { it.id == ChuggingBarrel.DISASSEMBLED.asRSCM() && it.vars == 0 } ?: return
        if (barrel.owns(player) || player.invMap["inv.bank"]?.any { it?.id == ChuggingBarrel.ITEM.asRSCM() } == true) { mes("You already own an assembled barrel."); return }
        val origin = coords; anim("seq.human_herbing_grind"); delay(3); resetAnim()
        if (inv[slot] !== item || coords != origin || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || barrel.owns(player) || player.invMap["inv.bank"]?.any { it?.id == ChuggingBarrel.ITEM.asRSCM() } == true) return
        if (invReplaceSlot(inv, slot, 1, ServerCacheManager.getItem(ChuggingBarrel.ITEM.asRSCM())!!).success) mes("You assemble the Chugging barrel.")
    }
    private suspend fun ProtectedAccess.dismantle(slot: Int) {
        val item = inv[slot]?.takeIf { it.id == ChuggingBarrel.ITEM.asRSCM() } ?: return
        val origin = coords
        if (barrel.contents(player).any { it != null } && !choice2("Yes", true, "No", false, title = "All stored potions will be lost. Disassemble?")) return
        if (inv[slot] !== item || coords != origin || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
        anim("seq.human_herbing_grind"); delay(3); resetAnim()
        if (inv[slot] !== item || coords != origin || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return
        if (invReplaceSlot(inv, slot, 1, ServerCacheManager.getItem(ChuggingBarrel.DISASSEMBLED.asRSCM())!!).success) { barrel.clear(player); mes("You disassemble the Chugging barrel.") }
    }
    private fun ProtectedAccess.drink() {
        if (!ConsumableDelayState.canConsume(this, ConsumableType.POTION)) return
        var drank = false; var delay = 0; var combat = 0
        for (slot in 0..4) {
            val items = barrel.contents(player).toMutableList(); val stored = items[slot] ?: continue
            val potion = PotionFamilies.byId(stored.id)?.potion ?: continue
            if (potion.minigameOnly.isNotEmpty() || potion.raidOnly.isNotEmpty() || potion.wildernessOnly && player.vars["varbit.inside_wilderness"] == 0 || !effects.supports(potion.effect) || !effects.canApply(this, potion.effect)) continue
            effects.apply(this, potion.effect); effects.healMix(this, potion.heal)
            items[slot] = if (stored.count == 1) null else stored.copy(count = stored.count - 1); barrel.write(player, items)
            mes(if (stored.count == 1) "You have finished all your doses of ${potion.name.lowercase()} in your barrel." else "You drink some of your ${potion.name.lowercase()}.")
            drank = true; delay = maxOf(delay, potion.drinkDelay); combat = maxOf(combat, potion.combatDelay)
        }
        if (!drank) { mes("You have no potions in your barrel that you can drink."); return }
        anim("seq.prepot_device_drink_full"); soundSynth(2401); player.resetFaceEntity()
        ConsumableDelayState.recordConsumption(this, ConsumableType.POTION, delay, combat)
    }
    private fun ProtectedAccess.open() {
        refresh(); ifOpenMainModal(MAIN); runClientScript(7338, ChuggingBarrel.ITEM.asRSCM())
        for ((component, slots) in listOf(CONTENTS to 0..4, INVENTORY to 0..27)) ifSetEvents(component, slots, IfEvent.Op1, IfEvent.Op2, IfEvent.Op3, IfEvent.Op4, IfEvent.Op5, IfEvent.Op6, IfEvent.Op7, IfEvent.Op8, IfEvent.Op10, IfEvent.DragTarget)
    }
    private fun ProtectedAccess.refresh() { val display = player.invMap.getOrPut(ChuggingBarrel.DISPLAY); display.fillNulls(); for ((slot, item) in barrel.contents(player).withIndex()) if (item != null) display[slot] = InvObj(ServerCacheManager.getItem(item.id)!!, item.count); invTransmit(display); invTransmit(inv) }
    private suspend fun ProtectedAccess.contentOp(event: IfModalButton) {
        if (!valid() || event.comsub !in 0..4) return
        val item = barrel.contents(player)[event.comsub] ?: return
        if (event.obj?.id?.let { it != item.id } == true) return
        if (event.op == IfButtonOp.Op10) { mes(ServerCacheManager.getItem(item.id)!!.examine); return }
        val original = barrel.contents(player); val origin = coords
        val requested = when (event.op) { IfButtonOp.Op1 -> PotionFamilies.byId(item.id)!!.maximum; IfButtonOp.Op2, IfButtonOp.Op5 -> 1; IfButtonOp.Op3, IfButtonOp.Op7 -> Int.MAX_VALUE; IfButtonOp.Op4, IfButtonOp.Op8 -> countDialog(); IfButtonOp.Op6 -> 5; else -> return }
        ifCloseChat(); if (!valid() || !player.ui.containsModal(MAIN) || coords != origin || barrel.contents(player) != original) return
        with(barrel) { if (event.op in listOf(IfButtonOp.Op5, IfButtonOp.Op6, IfButtonOp.Op7, IfButtonOp.Op8)) { val family = PotionFamilies.byId(item.id)!!; val source = inv.indexOfFirst { it != null && family.dose(it.id) > 0 }; if (source >= 0) store(source, requested, true) } else withdraw(event.comsub, requested) }; refresh()
    }
    private suspend fun ProtectedAccess.inventoryOp(event: IfModalButton) {
        if (!valid() || event.comsub !in inv.indices) return
        val item = inv[event.comsub] ?: return
        if (event.obj?.id?.let { it != item.id } == true) return
        if (event.op == IfButtonOp.Op10) { mes(ServerCacheManager.getItem(item.id)!!.examine); return }
        val origin = coords
        val quantity = when (event.op) { IfButtonOp.Op1, IfButtonOp.Op5 -> 1; IfButtonOp.Op2, IfButtonOp.Op6 -> 5; IfButtonOp.Op3, IfButtonOp.Op7 -> Int.MAX_VALUE; IfButtonOp.Op4, IfButtonOp.Op8 -> countDialog(); else -> return }
        ifCloseChat(); if (!valid() || !player.ui.containsModal(MAIN) || coords != origin || inv[event.comsub] !== item) return
        with(barrel) { store(event.comsub, quantity, event.op in listOf(IfButtonOp.Op5, IfButtonOp.Op6, IfButtonOp.Op7, IfButtonOp.Op8)) }; refresh()
    }
    companion object {
        const val MAIN = "interface.prepot_device"
        const val CONTENTS = "component.prepot_device:device_contents"
        const val INVENTORY = "component.prepot_device:inventory_contents"
    }
}
