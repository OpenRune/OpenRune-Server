package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WestArdougneGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.martha_rehnison") { startDialogue(it.npc) { martha() } }
        onOpNpc1("npc.milli") { startDialogue(it.npc) { milli() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Martha_Rehnison (after Plague City)
    // TODO(content): [quest:song_of_the_elves] "Good day friend." after the quest
    private suspend fun Dialogue.martha() {
        chatNpc(quiz, "Any luck finding Elena yet?")
        chatPlayer(happy, "Yes, she is safe at home now.")
        chatNpc(happy, "That's good to hear, she helped us a lot.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Milli_Rehnison (after Plague City)
    // TODO(content): [quest:song_of_the_elves] "I hope Elena comes to visit soon." after the quest
    private suspend fun Dialogue.milli() {
        chatNpc(quiz, "Have you found Elena yet?")
        chatPlayer(happy, "Yes, she's safe at home.")
        chatNpc(happy, "I hope she comes and visits sometime.")
        chatPlayer(neutral, "Maybe.")
    }
}
