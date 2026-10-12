package org.rsmod.content.areas.zeah.hosidius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_depths_of_despair] the house's lines while the quest is in progress
class HosidiusHouse : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidiusquest_lord") { startDialogue(it.npc) { lordKandur() } }
        onOpNpc1("npc.hosidiusquest_lady") { startDialogue(it.npc) { elena() } }
        onOpNpc1("npc.hosidius_pandur") { startDialogue(it.npc) { pandur() } }
        onOpNpc1("npc.hosidiusquest_butler") { startDialogue(it.npc) { butlerJarvis() } }
        onOpNpc1("npc.hosidiusquest_chef") { startDialogue(it.npc) { chefOlivia() } }
        onOpNpc1("npc.hosidiusquest_son_home") { startDialogue(it.npc) { arturHome() } }
        onOpNpc1("npc.akd_king") { startDialogue(it.npc) { kingArtur() } }
        onOpNpc1("npc.akd_elena_hosidius") { startDialogue(it.npc) { elenaAfterKingdom() } }
        onOpNpc1("npc.akd_pandur_hosidius") { startDialogue(it.npc) { lordPandur() } }
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
        if (!QuestRequirements.hasCompleted(player, DEPTHS_OF_DESPAIR)) {
            chatNpc(neutral, "Good day to you.")
            return
        }
        chatNpc(
            neutral,
            "Thank you for finding our son, you've done this kingdom a great service.",
        )
        chatPlayer(neutral, "It's no...")
        chatNpc(
            neutral,
            "I do wonder if his father could have done more though. My brother would have had " +
                "the entire army out searching.",
        )
        chatPlayer(quiz, "Your brother...")
        chatNpc(
            neutral,
            "My brother is Lord Shiro Shayzien. He commands the Shayzien Army. A luxury my " +
                "husband doesn't have.",
        )
        chatPlayer(quiz, "So you...")
        chatNpc(
            shifty,
            "Maybe it doesn't always have to be that way. Things may well soon be different. Now " +
                "be on your way, I have things to do.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Elena_Hosidius (After completing A Kingdom
    //  Divided)
    private suspend fun Dialogue.elenaAfterKingdom() {
        if (!QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED)) {
            // TODO(content): [quest:a_kingdom_divided] her dialogue during the quest
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Isn't this wonderful! My son on the throne. I always knew he'd do great things.",
        )
        chatPlayer(quiz, "Shame about his father though, right?")
        chatNpc(sad, "Oh yes, terrible business. It breaks Artur's heart not to have him around.")
        chatNpc(happy, "Still, always best to focus on the positives I say.")
        chatPlayer(neutral, "I guess.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Artur_Hosidius
    // TODO(content): [verify:in_game] the transcript doesn't mark which lines are options; the
    //  two menus here are read from its "(Shows other options)" layout
    private suspend fun Dialogue.arturHome() {
        if (!QuestRequirements.hasCompleted(player, DEPTHS_OF_DESPAIR)) {
            // TODO(content): [quest:the_depths_of_despair] his dialogue during the quest
            return
        }
        chatPlayer(happy, "Hello again Artur.")
        chatNpc(
            happy,
            "Hello ${player.displayName}. Thanks for your help in recovering the Accord.",
        )
        chatPlayer(quiz, "No problem. Why is the Accord important?")
        chatNpc(
            neutral,
            "The signing of the Accord marked the start of a golden age. Once the Royal Lords " +
                "replaced the Council of Elders, Kourend prospered like never before.",
        )
        chatPlayer(quiz, "But that was a long time ago. Why does it matter now?")
        chatNpc(
            neutral,
            "Because I think it's time for another golden age. The Kourend Council, like the " +
                "Council of Elders before them, have failed to do a good job in running this " +
                "kingdom.",
        )
        chatPlayer(quiz, "And you think the five houses would do a better job?")
        chatNpc(neutral, "Exactly.")
        val option =
            choice3(
                "What makes you say the council have done a bad job?",
                1,
                "Fair enough. See you later.",
                2,
                "Goodbye.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What makes you say the Council have done a bad job?")
                chatNpc(
                    angry,
                    "More crime. Fewer homes. More illness. Loss of jobs. The list goes on.",
                )
                chatPlayer(
                    quiz,
                    "But aren't the five houses partially to blame as well? They rule the cities.",
                )
                chatNpc(
                    neutral,
                    "The Council sets the laws and the Council sets the budgets. We have to do " +
                        "as they say, just like everyone else.",
                )
                chatPlayer(neutral, "Hmmm, fair enough. Anyway, we'd better get off.")
            }
            2 -> chatPlayer(neutral, "Fair enough. See you later.")
            else -> chatPlayer(neutral, "Goodbye.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Artur_Hosidius (After the completion of A
    //  Kingdom Divided)
    // TODO(content): [system:item_reclaim] "Before you go, my father would have wanted you to have
    //  this." and Lunch by the Lancalliums for players who lost it
    private suspend fun Dialogue.kingArtur() {
        if (!QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED)) {
            // TODO(content): [quest:a_kingdom_divided] his dialogue during the quest
            return
        }
        chatPlayer(
            happy,
            "Hello there Artur. Actually... I guess I should say Your Majesty now.",
        )
        chatNpc(happy, "For you friend, Artur is fine.")
        chatNpcSpecific(
            ELENA_TITLE,
            ELENA,
            angry,
            "You should not dispense formalities Artur. A king should command respect.",
        )
        chatNpc(neutral, "I can command respect without using fancy titles, mother.")
        chatPlayer(quiz, "So, how are you doing?")
        chatNpc(
            sad,
            "I have a lot of responsibility now. There's much to learn and not much time to " +
                "learn it in. The loss of father still weighs heavily on me as well.",
        )
        chatNpc(
            neutral,
            "Still, I am well supported by those around me. I'm sure that with their help, I can " +
                "be the ruler the people deserve.",
        )
        chatPlayer(
            happy,
            "I'm sure you can as well. Anyway, I'll let you get on with your work.",
        )
        chatNpc(happy, "Until next time.")
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

    // https://oldschool.runescape.wiki/w/Transcript:Pandur_Hosidius (After the completion of A
    //  Kingdom Divided)
    private suspend fun Dialogue.lordPandur() {
        if (!QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED)) {
            // TODO(content): [quest:a_kingdom_divided] his dialogue during the quest
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(
            drunk,
            "Ohh, I know you. You're the one that helped father take the throne! Of course, now " +
                "it's my brothers throne instead.",
        )
        chatPlayer(
            quiz,
            "Which I'm guessing makes you the new Lord Hosidius? How's that going?",
        )
        chatNpc(
            drunk,
            "Boring! Things were much more fun when my only job was to taste the wine and get " +
                "drunk.",
        )
        chatPlayer(neutral, "You still seem to be enjoying the wine just fine.")
        chatNpc(drunk, "Yes, but now I have to do other things as well.")
        chatPlayer(neutral, "It must be very hard for you.")
        chatNpc(drunk, "It is. Now, if you'll excuse me, I'm going to have another drink.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Butler_Jarvis
    private suspend fun Dialogue.butlerJarvis() {
        val title = if (access.isBodyTypeA()) "sir" else "madam"
        when {
            QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED) ->
                chatNpc(
                    neutral,
                    "Good day $title. I'm afraid I can't stop to talk. The new Lord Hosidius has " +
                        "plenty of work for me.",
                )
            QuestRequirements.hasCompleted(player, DEPTHS_OF_DESPAIR) ->
                chatNpc(
                    happy,
                    "Good day $title. On behalf of the Hosidius family, I thank you for bringing " +
                        "Artur home.",
                )
            else -> chatNpc(neutral, "Good day $title.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Chef_Olivia
    private suspend fun Dialogue.chefOlivia() {
        when {
            QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED) -> {
                chatPlayer(happy, "Hello.")
                chatNpc(
                    sad,
                    "Hello there. It's sad not having Artur around anymore, not to mention the " +
                        "awful business with his father. I hope he's coping okay with that.",
                )
                chatPlayer(neutral, "I'm sure he'll be fine.")
            }
            QuestRequirements.hasCompleted(player, DEPTHS_OF_DESPAIR) ->
                chatNpc(happy, "Thank you for bringing Artur home safe.")
            else -> chatNpc(neutral, "Sorry, I can't stop to talk.")
        }
    }

    private companion object {
        const val DEPTHS_OF_DESPAIR = "quest_depthsofdespair"
        const val KINGDOM_DIVIDED = "quest_kingdomdivided"
        const val ELENA = "npc.akd_elena_hosidius_vis"
        const val ELENA_TITLE = "Elena Hosidius"
    }
}
