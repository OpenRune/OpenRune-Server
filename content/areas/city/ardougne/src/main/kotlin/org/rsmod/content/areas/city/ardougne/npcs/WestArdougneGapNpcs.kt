package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WestArdougneGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.martha_rehnison") { startDialogue(it.npc) { martha() } }
        onOpNpc1("npc.milli") { startDialogue(it.npc) { milli() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Martha_Rehnison
    private suspend fun Dialogue.martha() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(happy, "Good day friend.")
            return
        }
        chatNpc(quiz, "Any luck finding Elena yet?")
        chatPlayer(happy, "Yes, she is safe at home now.")
        chatNpc(happy, "That's good to hear, she helped us a lot.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Milli_Rehnison
    private suspend fun Dialogue.milli() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(happy, "I hope Elena comes to visit soon.")
            return
        }
        chatNpc(quiz, "Have you found Elena yet?")
        chatPlayer(happy, "Yes, she's safe at home.")
        chatNpc(happy, "I hope she comes and visits sometime.")
        chatPlayer(neutral, "Maybe.")
    }

    private companion object {
        const val SONG_OF_THE_ELVES = "quest_songoftheelves"
    }
}
