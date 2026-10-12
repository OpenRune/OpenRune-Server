package org.rsmod.content.areas.misc.morytania.mortmyre

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MortMyreLocals : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.route_cyreg_paddlehorn") { startDialogue(it.npc) { cyreg() } }
        onOpNpc1("npc.route_curpile_fyod_child") { startDialogue(it.npc) { curpile() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cyreg_Paddlehorn
    // TODO(content): [quest:in_search_of_the_myreque] the in-quest dialogue
    // TODO(content): [system:swamp_boaty] boarding the boat for 10 coins
    private suspend fun Dialogue.cyreg() {
        if (QuestRequirements.hasCompleted(player, BLOOD_MOON_RISES)) {
            chatNpc(happy, "Oh, it's you. I was actually thinking about you recently.")
            chatPlayer(quiz, "How so?")
            chatNpc(
                neutral,
                "Well, if it wasn't for me, you might never have found the Myreque. On one hand, " +
                    "that might mean they'd not have ended up getting killed, but on the other, " +
                    "the Drakans might never have been overthrown.",
            )
            chatPlayer(neutral, "That's an interesting way of thinking about it.")
            chatNpc(
                happy,
                "I suppose the important part is that the Drakans have gone, and in some ways, " +
                    "I'm to thank.",
            )
            chatPlayer(neutral, "Yes... I suppose so.")
            return
        }
        if (QuestRequirements.hasCompleted(player, SINS_OF_THE_FATHER)) {
            chatPlayer(happy, "Thanks for your help in finding the Myreque.")
            chatNpc(
                sad,
                "Well, I'd like to say that you're welcome. But what does it matter now? The " +
                    "Drakans killed the whole lot of them.",
            )
            return
        }
        if (QuestRequirements.hasCompleted(player, IN_SEARCH_OF_THE_MYREQUE)) {
            chatPlayer(happy, "Thanks for your help in finding the Myreque.")
            chatNpc(
                sad,
                "Well, I'd like to say that you're welcome. But I heard that you led Vanstrom " +
                    "straight to their hideout. I feel so guilty.",
            )
            chatPlayer(sad, "Hmm, yeah, I know how you feel.")
            return
        }
        chatNpc(
            neutral,
            "Hello there friend. I'm afraid all boat rides are out of service due to the high " +
                "ghast activity in Mort Myre.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Curpile_Fyod
    // TODO(content): [quest:in_search_of_the_myreque] the dialogue before the quest is complete
    private suspend fun Dialogue.curpile() {
        chatNpc(angry, "Hey. Leave me be. I'm guardin' this place.")
    }

    private companion object {
        const val IN_SEARCH_OF_THE_MYREQUE = "quest_insearchofthemyreque"
        const val SINS_OF_THE_FATHER = "quest_sinsofthefather"
        const val BLOOD_MOON_RISES = "quest_bloodmoonrises"
    }
}
