package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Captain Dalbur, who runs the Al Kharid end of the gnome glider network. */
class GnomeGliderPilot : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.pilot_al_kharid") { openGliderMap() }
    }

    private fun ProtectedAccess.openGliderMap() {
        ifOpenMainModal("interface.glidermap")
    }
}
