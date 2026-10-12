package org.rsmod.content.areas.city.taverley.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class TaverleyGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.druid") { startDialogue(it.npc) { druid() } }
        onOpNpc1("npc.lord_daquarius") { startDialogue(it.npc) { lordDaquarius() } }
        onOpNpc1("npc.grim_miazrqa") { startDialogue(it.npc) { miazrqa() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Druid
    private suspend fun Dialogue.druid() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Good day to you, may nature smile upon you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Daquarius
    private suspend fun Dialogue.lordDaquarius() {
        chatPlayer(happy, "Hello.")
        chatNpc(angry, "What want you, with the Lord of the Kinshra? Speak!")
        chatPlayer(worried, "Erm, nothing actually.")
        chatNpc(angry, "Then begone knave, before I have my Knights put you to the sword!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Miazrqa (before Grim Tales)
    // TODO(content): [quest:grim_tales] after talking to Rupert, and after the quest
    private suspend fun Dialogue.miazrqa() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            neutral,
            "Oh, hello there. I'm afraid I don't have time to talk right now. I'm rather busy. " +
                "Come back some other time please.",
        )
        chatPlayer(neutral, "Ok, sorry.")
    }
}
