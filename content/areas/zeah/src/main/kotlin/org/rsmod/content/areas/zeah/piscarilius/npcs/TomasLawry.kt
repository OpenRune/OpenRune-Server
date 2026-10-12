package org.rsmod.content.areas.zeah.piscarilius.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Tomas_Lawry
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:the_queen_of_thieves] "Absolutely."/"Nah." start offer, the quest's
//  requirement check on "I'm looking for a quest.", and the during-quest dialogue
class TomasLawry : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.piscquest_official") { startDialogue(it.npc) { tomasLawry() } }
    }

    private suspend fun Dialogue.tomasLawry() {
        when {
            QuestRequirements.hasCompleted(player, KINGDOM_DIVIDED) -> afterKingdomDivided()
            QuestRequirements.hasCompleted(player, QUEEN_OF_THIEVES) -> afterQueenOfThieves()
            else -> beforeQueenOfThieves()
        }
    }

    private suspend fun Dialogue.afterQueenOfThieves() {
        chatPlayer(happy, "Hello Tomas. How's things?")
        chatNpc(
            happy,
            "Very well thank you. My work in bringing down Councillor Hughes means that I'm " +
                "finally starting to get the recognition that I deserve.",
        )
        chatPlayer(angry, "But that was my work!")
        chatNpc(
            neutral,
            "Yes, yes, very good job. I'll make sure to mention you in my acceptance speech when " +
                "I'm voted in as her replacement.",
        )
    }

    private suspend fun Dialogue.afterKingdomDivided() {
        chatPlayer(happy, "Hello Tomas.")
        chatNpc(sad, "Oh... Hello there.")
        chatPlayer(quiz, "Something wrong?")
        chatNpc(
            sad,
            "I'm ruined. Everything was perfect under the Council, but now I'm right back in the " +
                "dirt again.",
        )
        chatPlayer(
            neutral,
            "You were very close to the Council. You should be glad you got to keep your job at " +
                "all.",
        )
        chatNpc(
            sad,
            "But I didn't keep my job. Now I spend every day doing boring administrative work. " +
                "It's a nightmare.",
        )
        chatPlayer(happy, "Come now Tomas, it can't be all bad. Show a little enthusiasm.")
        chatNpc(sad, "Eugh.")
    }

    private suspend fun Dialogue.beforeQueenOfThieves() {
        chatPlayer(happy, "Hello.")
        chatNpc(neutral, "Greetings. What can I do for you?")
        val option =
            choice3(
                "What are you doing here?",
                1,
                "I'm looking for a quest.",
                2,
                "Nothing thanks.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you doing here?")
                chatNpc(neutral, "I'm conducting important business here for the Kourend Council.")
                investigationMenu()
            }
            2 -> {
                chatPlayer(quiz, "I'm looking for a quest.")
                chatNpc(
                    neutral,
                    "Well there is something I could use some help with. I'm here on behalf of " +
                        "the Kourend Council to investigate some unusual happenings in Port " +
                        "Piscarilius.",
                )
                val next =
                    choice3(
                        "What are you investigating?",
                        1,
                        "What's the Kourend Council?",
                        2,
                        "Well good luck with that.",
                        3,
                    )
                when (next) {
                    1 -> investigation()
                    2 -> kourendCouncil()
                    else -> chatPlayer(neutral, "Well good luck with that.")
                }
            }
            else -> {
                chatPlayer(neutral, "Nothing thanks.")
                chatNpc(neutral, "Very well. Until next time.")
            }
        }
    }

    private suspend fun Dialogue.investigationMenu() {
        if (choice2("Anything I can help with?", true, "What's the Kourend Council?", false)) {
            chatPlayer(quiz, "Is there anything I can help you with?")
            chatNpc(
                neutral,
                "There is something I could use some help with. I'm here on behalf of the " +
                    "Kourend Council to investigate some unusual happenings in Port Piscarilius.",
            )
            investigation()
        } else {
            kourendCouncil()
        }
    }

    private suspend fun Dialogue.kourendCouncil() {
        chatPlayer(quiz, "What's the Kourend Council?")
        chatNpc(
            neutral,
            "Although it's called a kingdom, Great Kourend hasn't actually had a king for many " +
                "years. These days Kourend is a republic and it is ruled by the Kourend Council.",
        )
        chatNpc(
            neutral,
            "The Council consists of four councillors who are elected by the people every four " +
                "years. The current councillors are Councillor Hughes, Councillor Unkar, " +
                "Councillor Orson and Councillor Andrews.",
        )
        chatPlayer(quiz, "I see. So what are you investigating?")
        gangs()
    }

    private suspend fun Dialogue.investigation() {
        chatPlayer(quiz, "What are you investigating?")
        gangs()
    }

    private suspend fun Dialogue.gangs() {
        chatNpc(
            neutral,
            "Port Piscarilius is home to various gangs. They're a nuisance, but up until now we " +
                "haven't had much trouble in dealing with them.",
        )
        chatPlayer(quiz, "So what's the problem?")
        chatNpc(
            neutral,
            "A new gang calling themselves the Saviours of Kourend has recently formed. That's " +
                "not surprising as gangs come and go all of the time, but this one is different.",
        )
        chatPlayer(quiz, "How so?")
        chatNpc(
            worried,
            "The Saviours of Kourend have been absorbing many of the other gangs. Within a few " +
                "weeks they've already become the largest gang in Kourend and more join them " +
                "each day.",
        )
        chatNpc(
            neutral,
            "The talk amongst the people is that it's all down to their leader, a mysterious " +
                "figure known only as the Queen of Thieves.",
        )
        chatPlayer(
            quiz,
            "Nice name. Why doesn't the ruler of Port Piscarilius do something about this?",
        )
        chatNpc(
            angry,
            "Shauna Piscarilius is the ruler of Port Piscarilius, but she spends most of her " +
                "time galavanting around in that boat of hers rather than looking after her home.",
        )
        chatPlayer(quiz, "Fair enough. So what do you need me for?")
        chatNpc(
            neutral,
            "Well my original plan was to infiltrate the gang to get close to the Queen and " +
                "capture her. However, I've been unable to make any progress. Apparently, I " +
                "don't look the part.",
        )
        chatPlayer(
            laugh,
            "Yeah not sure what that's all about. You clearly fit right in here.",
        )
        chatNpc(
            neutral,
            "Quite right. They're quite insistent though, so someone else will need to give it a " +
                "go. You look like you might be just the type they're after.",
        )
        chatPlayer(confused, "What are you trying to say?")
        chatNpc(neutral, "That you look poor.")
        chatPlayer(sad, "Oh...")
        chatNpc(
            neutral,
            "Anyway if you're up for the task you'll need to infiltrate the Saviours of Kourend, " +
                "find the Queen of Thieves, apprehend her and then bring her to me.",
        )
        chatNpc(quiz, "So are you up for it?")
    }

    private companion object {
        const val QUEEN_OF_THIEVES = "quest_queenofthieves"
        const val KINGDOM_DIVIDED = "quest_kingdomdivided"
    }
}
