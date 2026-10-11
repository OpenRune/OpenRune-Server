package org.rsmod.content.other.containers

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.invtx.*
import org.rsmod.api.player.interact.HeldInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.*
import org.rsmod.api.script.advanced.onDropHeld
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class SteelKeyRingScript @Inject constructor(private val held: HeldInteractions) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(SteelKeyRing.ITEM) {
            if (!active()) return@onOpHeld1
            ifOpenMain("interface.favour_keyring")
            for (button in listOf("next", "next_highlight", "previous", "previous_highlight", "remove", "remove_text")) ifSetEvents(comp(button), -1..0, IfEvent.Op1)
            refresh()
        }
        for ((index, entry) in SteelKeyRing.keys.withIndex()) onOpHeldU(SteelKeyRing.ITEM, entry.first) {
            if (!active() || inv[it.firstSlot]?.id != SteelKeyRing.ITEM.asRSCM() || inv[it.secondSlot]?.id != entry.first.asRSCM()) return@onOpHeldU
            if (SteelKeyRing.has(player, index)) { mes("That key is already on your key ring."); return@onOpHeldU }
            if (player.invDel(inv, entry.first.asRSCM(), 1, slot = it.secondSlot).success) {
                SteelKeyRing.set(player, index, true); mes("You add the ${getObjName(entry.first).lowercase()} to your key ring.")
            }
        }
        for (button in listOf("next", "next_highlight")) onIfModalButton(comp(button)) { move(1) }
        for (button in listOf("previous", "previous_highlight")) onIfModalButton(comp(button)) { move(-1) }
        for (button in listOf("remove", "remove_text")) onIfModalButton(comp(button)) {
            if (!active()) { ifClose(); return@onIfModalButton }
            val stored = SteelKeyRing.stored(player); val index = stored.getOrNull(selected(stored.size)) ?: return@onIfModalButton
            val key = SteelKeyRing.keys[index].first
            if (player.invAdd(inv, key.asRSCM(), 1).success) { SteelKeyRing.set(player, index, false); mes("You remove the ${getObjName(key).lowercase()} from your key ring."); refresh() } else mes("Your inventory is too full to remove that key.")
        }
        onOpHeld5(SteelKeyRing.ITEM) {
            val ring = inv[it.slot] ?: return@onOpHeld5
            if (ring.id != SteelKeyRing.ITEM.asRSCM()) return@onOpHeld5
            if (SteelKeyRing.mask(player) != 0 && menu("Dropping your last key ring will lose its keys.", "Drop", "Cancel") != 0) return@onOpHeld5
            if (active() && inv[it.slot] === ring) held.drop(this, inv, it.slot)
        }
        onDropHeld { if (type.id == SteelKeyRing.ITEM.asRSCM() && !SteelKeyRing.backed(player)) player.attr.remove(SteelKeyRing.CONTENTS) }
    }
    private fun ProtectedAccess.active() = SteelKeyRing.owned(player) && player.hitpoints > 0 && !player.pendingLogout && !player.loggingOut
    private fun ProtectedAccess.selected(size: Int) = (player.attr[SteelKeyRing.SELECTED] ?: 0).coerceIn(0, maxOf(0, size - 1))
    private fun ProtectedAccess.move(delta: Int) {
        if (!active()) { ifClose(); return }
        val count = SteelKeyRing.stored(player).size
        if (count > 0) player.attr[SteelKeyRing.SELECTED] = (selected(count) + delta + count) % count
        refresh()
    }
    private fun ProtectedAccess.refresh() {
        val stored = SteelKeyRing.stored(player); val cursor = selected(stored.size)
        player.attr[SteelKeyRing.SELECTED] = cursor
        ifSetText(comp("keytotal"), "Keys: ${stored.size}/13")
        val entry = stored.getOrNull(cursor)?.let { SteelKeyRing.keys[it] }
        ifSetText(comp("name"), entry?.first?.let(::getObjName) ?: "No keys stored")
        ifSetText(comp("desc"), entry?.second ?: "Use a reusable key on your key ring to add it.")
        ifSetHide(comp("main"), entry == null); ifSetHide(comp("remove"), entry == null); ifSetHide(comp("remove_text"), entry == null)
        if (entry != null) ifSetObj(comp("main"), entry.first, 500)
        for (n in 1..5) {
            val neighbour = stored.getOrNull(cursor + n)
            ifSetHide(comp("plus$n"), neighbour == null)
            if (neighbour != null) ifSetObj(comp("plus$n"), SteelKeyRing.keys[neighbour].first, 400)
        }
    }
    private fun getObjName(key: String) = ServerCacheManager.getItem(key.asRSCM())!!.name
    private fun comp(name: String) = "component.favour_keyring:$name"
}
