package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LarryRellekka : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.peng_larry_rell") { startDialogue(it.npc) { larry() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Larry (At Rellekka boat, before Cold War)
    // TODO(content): [quest:cold_war] the boat to the iceberg and the clockwork book after the
    // quest
    private suspend fun Dialogue.larry() {
        chatNpc(angry, "What? Did you come to tell me I'm crazy too?")
        chatPlayer(neutral, "No, I...")
        chatNpc(angry, "Oh, I don't have time for your lies. Go away")
    }
}
