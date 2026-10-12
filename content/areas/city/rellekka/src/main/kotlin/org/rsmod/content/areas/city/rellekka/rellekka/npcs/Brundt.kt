package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Brundt : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_brundt") { startDialogue(it.npc) { brundt() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brundt_the_Chieftain (before the Trials)
    // TODO(content): [quest:the_fremennik_trials] in-progress and after-quest dialogue
    // TODO(content): [quest:the_fremennik_exiles] Seal of Passage, mountain camp and quest
    //  dialogue
    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.brundt() {
        chatNpc(happy, "Greetings outerlander!")
        val option =
            choice3(
                "What is this place?",
                Topic.Place,
                "Do you have any quests?",
                Topic.Quests,
                "Nice hat!",
                Topic.Hat,
            )
        when (option) {
            Topic.Place -> whatIsThisPlace()
            Topic.Quests -> quests()
            Topic.Hat -> {
                chatPlayer(happy, "Nice hat!")
                chatNpc(
                    angry,
                    "Do not mock me outerlander; this helm has saved my life more times than I " +
                        "care to recall right now.",
                )
            }
            else -> return
        }
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(
            happy,
            "This place? Why, this is Rellekka! Homeland of all Fremennik! I do not recognise " +
                "your face outerlander; Where do you come from?",
        )
        chatPlayer(shifty, "That's not important...")
        chatNpc(
            neutral,
            "Hmmm... I will not press the issue then outerlander. How may my tribe and I help " +
                "you?",
        )
        while (true) {
            val option =
                choice5(
                    "Do you have any quests?",
                    Topic.Quests,
                    "Why will no-one talk to me?",
                    Topic.NoOneTalks,
                    "Why do you call me outerlander?",
                    Topic.Outerlander,
                    "I seek an army to destroy Asgarnia!",
                    Topic.Army,
                    "Who are the Fremennik?",
                    Topic.Fremennik,
                )
            when (option) {
                Topic.Quests -> return quests()
                Topic.NoOneTalks -> return whyWillNoOneTalk()
                Topic.Outerlander -> outerlander()
                Topic.Army -> army()
                Topic.Fremennik -> {
                    chatPlayer(quiz, "Who are the Fremennik?")
                    chatNpc(
                        neutral,
                        "It is the name we call ourselves. Outerlanders tend to call us " +
                            "'Barbarian', but they do not understand our ways...",
                    )
                }
                else -> return
            }
        }
    }

    private suspend fun Dialogue.quests() {
        chatPlayer(quiz, "Do you have any quests?")
        chatNpc(
            neutral,
            "Quests, you say outerlander? Well, I would not call it a quest as such, but if you " +
                "are brave of heart and strong of body, perhaps...",
        )
        chatNpc(neutral, "No, you would not be interested. Forget I said anything, outerlander.")
        val interested =
            choice2("Yes, I am interested.", true, "No, I'm not interested.", false)
        if (interested) {
            questOffer()
        } else {
            chatPlayer(bored, "You're right. I really couldn't care less.")
            chatNpc(
                sad,
                "It is as I thought outerlander. Your kind care nothing for the Fremennik, and " +
                    "we feel the same about you. This is the way of the world.",
            )
        }
    }

    private suspend fun Dialogue.whyWillNoOneTalk() {
        chatPlayer(quiz, "Why will no-one talk to me?")
        chatNpc(
            neutral,
            "Do not take it personally, outerlander! We are a simple people, and it is our " +
                "experience that keeping ourselves to ourselves is best. This is why speaking to " +
                "outerlanders is forbidden.",
        )
        chatNpc(
            neutral,
            "We do not wish to enter war with the outerlanders and their strange magics, so we " +
                "limit all unauthorised communication.",
        )
        chatPlayer(quiz, "Then how come you're talking to me?")
        chatNpc(
            neutral,
            "Ah, this is because I am the chieftain. I am the one who authorises contact. You " +
                "will not find many of my tribe so forthcoming with you, as I.",
        )
        chatPlayer(quiz, "Is there a way for you to authorise your tribe to talk to me then?")
        chatNpc(neutral, "Well, there is one way... but I doubt it is of any interest to you.")
        questOffer()
    }

    // TODO(content): [quest:the_fremennik_trials] "Start The Fremennik Trials quest?" offer, the
    //  start and the "No." reply
    private suspend fun Dialogue.questOffer() {
        chatPlayer(happy, "Actually, I would be very interested to hear what you have to offer.")
        chatNpc(
            shocked,
            "You would? These are unusual sentiments to hear from an outerlander! My suggestion " +
                "was going to be that if you crave adventure and battle,",
        )
        chatNpc(
            happy,
            "and your heart sings for glory, then perhaps you would be interested in joining our " +
                "clan, and becoming a Fremennik yourself?",
        )
        chatPlayer(quiz, "What would that involve exactly?")
        chatNpc(
            neutral,
            "Well, there are two ways to become a member of our clan and call yourself a " +
                "Fremennik: be born a Fremennik, or be voted in by our council of elders.",
        )
        chatPlayer(
            quiz,
            "Well, I think I've missed the first way, but how can I get the council of elders to " +
                "vote to let me join your clan?",
        )
        chatNpc(
            neutral,
            "Well, that I cannot answer myself. You will need to speak to each of them and see " +
                "what they require of you as proof of your dedication.",
        )
        chatNpc(
            neutral,
            "There are twelve council members around this village; you will need to gain a " +
                "majority vote of at least seven councillors in your favour.",
        )
        chatNpc(
            quiz,
            "So what say you? Give me the word, and I will tell all of my tribe of your " +
                "intentions, be they yea or nay.",
        )
    }

    private suspend fun Dialogue.outerlander() {
        chatPlayer(quiz, "Why do you call me outerlander?")
        chatNpc(
            neutral,
            "Do not take it as an insult, outerlander. For us, this town is the center of all. " +
                "Anyone not born here, or that we do not know and trust is an outerlander.",
        )
        chatPlayer(quiz, "So it basically means stranger?")
        chatNpc(
            neutral,
            "Aye, but that is an outerlander word. We do not judge someones ways by their name. " +
                "It simply means one from the outerlands, or one not from Rellekka.",
        )
        chatNpc(
            neutral,
            "I know our customs are foreign to you; perhaps you would like me to explain " +
                "something else?",
        )
    }

    private suspend fun Dialogue.army() {
        chatPlayer(angry, "I seek an army to destroy Asgarnia!")
        chatNpc(
            angry,
            "We will have no part in the squabbles of outerlander tribes. Their magics are " +
                "strange and deadly to us, as we learnt long ago.",
        )
        chatNpc(
            angry,
            "If this is all that you came here for, then you have had a wasted journey. If you " +
                "are looking for something else, then speak now for my patience dwindles.",
        )
    }

    private enum class Topic {
        Place,
        Quests,
        Hat,
        NoOneTalks,
        Outerlander,
        Army,
        Fremennik,
    }
}
