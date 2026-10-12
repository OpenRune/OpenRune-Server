package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Customer_(Dwarf)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class KeldagrimLibrary : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dwarf_city_library_customer") { startDialogue(it.npc) { customer() } }
    }

    private suspend fun Dialogue.customer() {
        chatPlayer(quiz, "Hello, can you help me?")
        chatNpc(angry, "Ssshhh, don't talk...")
        chatPlayer(quiz, "But I just want to ask you something.")
        chatNpc(angry, "Ssshhhh! Can't you read the sign?")
        chatPlayer(confused, "What sign??")
        chatNpc(angry, "Don't be a cheeky human!")
        chatPlayer(shifty, "Ssshhhh.")
    }
}
