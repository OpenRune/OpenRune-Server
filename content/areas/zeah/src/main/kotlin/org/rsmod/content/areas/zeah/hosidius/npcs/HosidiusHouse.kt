package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_depths_of_despair] Artur Hosidius and the during/after-quest lines
// TODO(content): [quest:a_kingdom_divided] after-quest lines and the akd_* house npcs
class HosidiusHouse : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidiusquest_lord") { startDialogue(it.npc) { lordKandur() } }
        onOpNpc1("npc.hosidiusquest_lady") { startDialogue(it.npc) { elena() } }
        onOpNpc1("npc.hosidius_pandur") { startDialogue(it.npc) { pandur() } }
        onOpNpc1("npc.hosidiusquest_butler") { startDialogue(it.npc) { butlerJarvis() } }
        onOpNpc1("npc.hosidiusquest_chef") { startDialogue(it.npc) { chefOlivia() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lord_Kandur_Hosidius
    // TODO(content): [quest:the_depths_of_despair] replacing a lost memoir page reward
    private suspend fun Dialogue.lordKandur() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello there.")
        while (true) {
            val option =
                choice4(
                    "Tell me about yourself.",
                    1,
                    "Tell me about Hosidius.",
                    2,
                    "Tell me about your plans.",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about yourself.")
                    chatNpc(
                        neutral,
                        "I am Kandur Hosidius and I am the lord of Hosidius, the city of farmers " +
                            "and chefs. I live here with my wife, Elena, and my two sons, Artur " +
                            "and Pandur.",
                    )
                    chatNpc(
                        neutral,
                        "My family and I have a peaceful existence here. That's mainly because I " +
                            "care not for the politics of the other houses. I instead devote my " +
                            "time to more meaningful pursuits.",
                    )
                    chatPlayer(quiz, "Such as?")
                    chatNpc(
                        neutral,
                        "Ensuring that Kourend is well fed. We may live in gentle times now but " +
                            "who knows how long it will be that way. Sooner or later another war " +
                            "or long winter will hit us. History has proven that.",
                    )
                    chatNpc(quiz, "Anything else I can help you with?")
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about Hosidius.")
                    chatNpc(
                        neutral,
                        "Hosidius is home to the farmers and chefs of Kourend. We are the main " +
                            "food supplier to the kingdom and we ensure that her people are fed.",
                    )
                    chatNpc(
                        neutral,
                        "We also own more land than the other Kourend houses. We need the extra " +
                            "space for all of our crops.",
                    )
                    chatPlayer(
                        quiz,
                        "Do the other houses not get annoyed at how much land you have?",
                    )
                    chatNpc(
                        neutral,
                        "Sometimes. They know it is necessary though. There was once a time when " +
                            "the land was shared equally. But then the great famine came, and " +
                            "Kourend did not have the food to last through it.",
                    )
                    chatNpc(
                        neutral,
                        "After that, more space was given to Hosidius. We've had the most land " +
                            "ever since.",
                    )
                    chatNpc(quiz, "Anything else I can help you with?")
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about your plans.")
                    chatNpc(
                        neutral,
                        "Plans? My only plan is to keep providing food to this kingdom.",
                    )
                    chatPlayer(
                        quiz,
                        "But what of the Royal Accord of Twill? You must have kept it for a " +
                            "reason.",
                    )
                    chatNpc(
                        neutral,
                        "I did. The Accord has the potential to be a powerful weapon, for words " +
                            "cut deeper than a blade ever can. However, I have no need to use " +
                            "this weapon, not unless the other houses give me reason to.",
                    )
                    chatNpc(quiz, "Anything else I can help you with?")
                }
                else -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(neutral, "Until next time.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elena_Hosidius
    private suspend fun Dialogue.elena() {
        chatNpc(neutral, "Good day to you.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Pandur_Hosidius
    private suspend fun Dialogue.pandur() {
        chatNpc(drunk, "What do you want?")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "What are these farming patches for?",
                    2,
                    "Have you been drinking?",
                    3,
                    "I'll leave you to it.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        drunk,
                        "What? Everyone knows who I am. I'm Pandur Hosidius, son of Lord Kandur " +
                            "Hosidius. You know him? The man who owns all this land? Well I'm " +
                            "his son.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What are these farming patches for?")
                    chatNpc(
                        drunk,
                        "This is our vinery. My father's cracked the secret of viticulture, and " +
                            "although our climate's too cold for most grapes, we've made a " +
                            "working vineyard here.",
                    )
                    // TODO(content): [verify:in_game] the favour-met reply is assumed since Kourend
                    //  favour was removed
                    chatNpc(
                        drunk,
                        "You seem to have got yourself a decent reputation around here, so you " +
                            "can grow grapes if you like. Old Gallow will look after them for you.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Have you been drinking?")
                    chatNpc(
                        drunk,
                        "I've got to test the produce. Ever since I was a child, I've been the " +
                            "official tester for the bacon, the beef, the pies, the beer, " +
                            "everything. Now there's wine!",
                    )
                    chatNpc(drunk, "* BURP *")
                }
                else -> {
                    chatPlayer(neutral, "I'll leave you to it.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Butler_Jarvis
    private suspend fun Dialogue.butlerJarvis() {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        chatNpc(neutral, "Good day $title.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chef_Olivia
    private suspend fun Dialogue.chefOlivia() {
        chatNpc(neutral, "Sorry, I can't stop to talk.")
    }
}
