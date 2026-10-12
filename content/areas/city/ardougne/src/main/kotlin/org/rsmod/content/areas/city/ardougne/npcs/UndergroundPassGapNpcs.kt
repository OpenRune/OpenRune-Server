package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class UndergroundPassGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.caveguide1") { startDialogue(it.npc) { koftik() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Koftik
    // TODO(content): [quest:underground_pass] the in-quest dialogue, and handing a broken Iban's
    //  staff back to a player without Iban's staff after the quest
    private suspend fun Dialogue.koftik() {
        if (QuestRequirements.hasCompleted(player, "quest_undergroundpass")) {
            chatPlayer(happy, "Hello Koftik.")
            chatNpc(happy, "Hello adventurer, how's things?")
            chatPlayer(happy, "Not bad, yourself?")
            chatNpc(neutral, "I'm good... just keeping an eye out.")
            return
        }
        chatPlayer(quiz, "Hello. What are you up to?")
        chatNpc(neutral, "Nothing exciting, just keeping an eye out.")
    }
}
