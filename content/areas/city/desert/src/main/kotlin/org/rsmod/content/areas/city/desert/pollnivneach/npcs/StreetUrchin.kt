package org.rsmod.content.areas.city.desert.pollnivneach.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Street_urchin (before The Feud)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_feud] payment and distraction dialogue after the quest
class StreetUrchin : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.feud_street_urchin") { startDialogue(it.npc) { urchin() } }
    }

    private suspend fun Dialogue.urchin() {
        chatPlayer(happy, "Hello there little fellow.")
        chatNpc(angry, "I'm not little and if you say that again I'll gut you.")
        chatPlayer(worried, "Easy there, I didn't mean to offend.")
        chatNpc(neutral, "I'll let you off this once. Well what do you want? Information?")
        chatPlayer(happy, "Yes, tell me about...")
        while (true) {
            val topic =
                choice4(
                    "The Menaphites",
                    1,
                    "The bandits",
                    2,
                    "The Mayor",
                    3,
                    "The town",
                    4,
                )
            when (topic) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the Menaphites.")
                    chatNpc(
                        neutral,
                        "They're a bad bunch, always starting fights down in the Asp and Snake. " +
                            "Word on the street is that they're led by some deranged priest. " +
                            "Nobody's ever seen them though - they do all their deals through " +
                            "Ali 'the",
                    )
                    chatNpc(
                        neutral,
                        "Operator'. I reckon they're up to something, but they're such a close " +
                            "knit group that little ever slips out.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about the bandits.")
                    chatNpc(
                        neutral,
                        "There's not much to say, really. They're just a group of local thugs led " +
                            "by the biggest and strongest amongst them. If they have any other " +
                            "plans, I don't know about it.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about the Mayor.")
                    chatNpc(
                        angry,
                        "The mayor is a spineless coward! He never stands up to anybody. That's " +
                            "why the Menaphites are so strong now! He let them stay, and then " +
                            "they started causing trouble. When he realised his mistake, he " +
                            "hired a band",
                    )
                    chatNpc(
                        angry,
                        "of local thugs to drive them out, but they saw how weak he was and " +
                            "turned on us too. Now we've got bandits and Menaphites, and the " +
                            "mayor isn't doing anything!",
                    )
                }
                4 -> {
                    chatPlayer(quiz, "Tell me about the town.")
                    chatNpc(
                        bored,
                        "It's boring! We're not as big as Menaphos or as powerful as Al Kharid, " +
                            "and we've got a really nasty crime problem. It's no wonder so many " +
                            "grown-ups have left.",
                    )
                }
            }
            chatNpc(neutral, "Do you need any other information?")
            if (!choice2("Yes tell me about...", true, "No thanks.", false)) {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Come back if you need any info about anything in the town.")
                chatPlayer(happy, "I will do thanks.")
                return
            }
            chatPlayer(happy, "Yes, tell me about...")
        }
    }
}
