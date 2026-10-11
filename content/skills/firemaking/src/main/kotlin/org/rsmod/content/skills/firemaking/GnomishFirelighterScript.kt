package org.rsmod.content.skills.firemaking

import org.rsmod.api.script.*
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

internal class GnomishFirelighterScript : PluginScript() {
    override fun ScriptContext.startup() {
        for (item in listOf(GnomishFirelighter.EMPTY, GnomishFirelighter.CHARGED)) {
            onOpHeld4(item) {
                val held = inv[it.slot] ?: return@onOpHeld4
                val colour = GnomishFirelighter.colour(held)
                mes(if (colour == null) "Your gnomish firelighter is empty." else "Your gnomish firelighter holds ${GnomishFirelighter.count(held)} ${colour.firelighter.name.lowercase()}s.")
            }
            onOpHeldU(item) {
                if (!GnomishFirelighter.fill(player, it.firstSlot, it.secondSlot)) mes("Use up to 1,000 firelighters of a single colour. Uncharge it before changing colour.")
            }
        }
        onOpHeld5(GnomishFirelighter.CHARGED) {
            if (!GnomishFirelighter.uncharge(player, it.slot)) mes("You need inventory space to recover your firelighters.")
        }
    }
}
