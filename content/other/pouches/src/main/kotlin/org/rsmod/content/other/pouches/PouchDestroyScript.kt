package org.rsmod.content.other.pouches

import org.rsmod.api.config.refs.params
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.events.interact.HeldDropEvents
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.script.onOpHeld5
import org.rsmod.content.other.pouches.lootingbag.LootingBags
import org.rsmod.content.other.pouches.runepouch.RunePouches
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Content-owned confirmation leaves the existing rune/bag spill consumers in charge of cleanup. */
internal class PouchDestroyScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (symbol in RunePouches.ALL + LootingBags.ALL) onOpHeld5(symbol) {
            if (player.hitpoints <= 0 || player.pendingLogout || player.loggingOut) return@onOpHeld5
            val original = inv[it.slot] ?: return@onOpHeld5
            val origin = coords
            if (!confirmDestroy(symbol, original.count, it.type.param(params.destroy_note_title), it.type.param(params.destroy_note_desc))) return@onOpHeld5
            if (coords != origin || player.hitpoints <= 0 || player.pendingLogout || player.loggingOut || inv[it.slot] !== original) return@onOpHeld5
            if (player.invDel(inv, original.id, original.count, slot = it.slot).success) publish(HeldDropEvents.Destroy(player, it.slot, original, symbol))
        }
    }
}
