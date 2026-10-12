package org.rsmod.content.areas.city.falador.npcs

import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Knight_(Falador_Party_Room)
class PartyRoomKnights : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.partyroom_knight") {
            startDialogue(it.npc) { mesbox("He is too busy dancing to talk!") }
        }
    }
}
