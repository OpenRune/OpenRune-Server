package org.rsmod.content.other.containers

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.util.Wearpos
import jakarta.inject.Inject
import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.*
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.interact.WornInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.worn.DizanasQuiver
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDestroyHeld
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class BoltPouchScript @Inject constructor(private val equipment: WornInteractions) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(BoltPouch.ITEM) { if (valid()) open() }
        onOpHeldU(BoltPouch.ITEM) { if (valid() && inv[it.firstSlot]?.id == BoltPouch.ITEM.asRSCM() && inv[it.secondSlot]?.id == it.second.id) { val moved = BoltPouch.fill(player, it.secondSlot); mes(if (moved > 0) "You add $moved bolts to your pouch." else "Your pouch cannot store that ammunition, or is full.") } }
        for ((slot, component) in BoltPouch.components.withIndex()) {
            onIfModalButton(component) {
                if (!valid() || !player.ui.containsModal(BoltPouch.MAIN) || it.op != IfButtonOp.Op1 || it.comsub !in -1..0) return@onIfModalButton
                val actual = if (slot == 0) worn[Wearpos.Quiver.slot] else BoltPouch.contents(player, slot)
                if (actual == null || it.obj?.id?.let { id -> id != actual.id } == true) return@onIfModalButton
                if (slot == 0) equipment.unequip(this, worn, Wearpos.Quiver.slot, inv) else BoltPouch.remove(player, slot)
            }
            for ((target, destination) in BoltPouch.components.withIndex()) onIfModalDrag(component, destination) {
                if (!valid() || !player.ui.containsModal(BoltPouch.MAIN) || slot == target || it.selectedSlot !in -1..0 || it.targetSlot !in -1..0) return@onIfModalDrag
                val first = if (slot == 0) worn[Wearpos.Quiver.slot] else BoltPouch.contents(player, slot)
                val second = if (target == 0) worn[Wearpos.Quiver.slot] else BoltPouch.contents(player, target)
                if (first?.id != it.selectedObj || second?.id != it.targetObj) return@onIfModalDrag
                if (slot == 0 || target == 0) {
                    if (DizanasQuiver.owns(player)) { mes("You cannot change ammo from your bolt pouch with Dizana's quiver on your person."); return@onIfModalDrag }
                    if (slot == 0 && second == null) {
                        if (first == null || !BoltPouch.accepted(first.id) || first.vars != 0 || first.count > BoltPouch.limit(first.id)) return@onIfModalDrag
                        if (player.invDel(worn, first.id, first.count, slot = Wearpos.Quiver.slot).success) { BoltPouch.write(player, target, first); player.rebuildAppearance() }
                    } else BoltPouch.toWorn(player, if (slot == 0) target else slot)
                } else { BoltPouch.write(player, slot, second); BoltPouch.write(player, target, first) }
            }
        }
        onIfOverlayButton("component.wornitems:extra_ammo_slot") {
            if (valid() && it.op == IfButtonOp.Op1 && it.comsub in -1..1 && !DizanasQuiver.owns(player) && it.obj?.id?.let { id -> id == BoltPouch.contents(player, 4)?.id } != false) BoltPouch.toWorn(player, 4)
        }
        onOpHeld5(BoltPouch.ITEM) {
            if (!valid()) return@onOpHeld5
            val original = inv[it.slot] ?: return@onOpHeld5; val origin = coords
            if (!confirmDestroy(BoltPouch.ITEM, original.count, it.type.param(params.destroy_note_title), it.type.param(params.destroy_note_desc))) return@onOpHeld5
            if (!valid() || coords != origin || inv[it.slot] !== original) return@onOpHeld5
            if (player.invDel(inv, original.id, original.count, slot = it.slot).success) publish(HeldDropEvents.Destroy(player, it.slot, original, BoltPouch.ITEM))
        }
        onDestroyHeld { if (type.asRSCM() == BoltPouch.ITEM.asRSCM() && !BoltPouch.owned(player) && player.invMap["inv.bank"]?.any { it?.id == BoltPouch.ITEM.asRSCM() } != true) BoltPouch.clear(player) }
        onOpNpc1("npc.xbows_sales_dwarf_keldegrim") {
            val origin = coords
            if (menu("Hirko", "Buy a bolt pouch (1,500 coins)", "Cancel") != 0 || coords != origin || !AmmoContainers.active(player)) return@onOpNpc1
            if (BoltPouch.owned(player) || player.invMap["inv.bank"]?.any { it?.id == BoltPouch.ITEM.asRSCM() } == true) { mes("You already own a bolt pouch."); return@onOpNpc1 }
            val result = player.invTransaction(inv) { val i = select(inv); delete(i, "obj.coins".asRSCM(), 1500); add(i, BoltPouch.ITEM.asRSCM(), 1) }
            mes(if (result.success) "Hirko gives you a bolt pouch." else "You need 1,500 coins and enough inventory space.")
        }
    }
    private fun ProtectedAccess.valid() = BoltPouch.owned(player) && AmmoContainers.active(player)
    private fun ProtectedAccess.open() {
        ifOpenMainModal(BoltPouch.MAIN)
        for (component in BoltPouch.components) ifSetEvents(component, -1..0, IfEvent.Op1, IfEvent.DragTarget)
    }
}
