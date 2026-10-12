package org.rsmod.content.areas.city.karamja.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ViyeldiCavesGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.ungadulu_good") { startDialogue(it.npc) { ungadulu() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ungadulu
    // TODO(content): [quest:legends_quest] dialogue after freeing him and before the quest ends;
    //  the after-quest dialogue is used since he is only freed in the quest
    private suspend fun Dialogue.ungadulu() {
        chatNpc(happy, "Your Legendary exploits are travelling the whole jungle.")
        chatNpc(
            neutral,
            "Gujuo has been to see me. He told me that you have been given a sacred totem pole. " +
                "It was constructed by one of my ancestors many moons ago. It is a noble prize " +
                "Bwana, you have earned it. Look after it well.",
        )
    }
}
