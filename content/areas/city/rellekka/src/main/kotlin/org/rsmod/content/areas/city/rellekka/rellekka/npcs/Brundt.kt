package org.rsmod.content.areas.city.rellekka.rellekka.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.areas.city.rellekka.brotherOrSister
import org.rsmod.content.areas.city.rellekka.choose
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Brundt : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_brundt") { startDialogue(it.npc) { brundt() } }
    }

    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.brundt() {
        if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
            fremennik()
        } else {
            outerlander()
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Brundt_the_Chieftain (before the Trials)
    // TODO(content): [quest:the_fremennik_trials] dialogue while the Trials are in progress
    private suspend fun Dialogue.outerlander() {
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
                Topic.Outerlander -> whyOuterlander()
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

    private suspend fun Dialogue.whyOuterlander() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Brundt_the_Chieftain (after the Trials)
    // TODO(content): [quest:lunar_diplomacy] "Ask about a Seal of Passage." once Lunar Diplomacy
    //  is started, and handing out the Seal of Passage
    // TODO(content): [quest:mountain_daughter] "Ask about the mountain camp." while Mountain
    //  Daughter is in progress (the wiki transcript is missing)
    // TODO(content): [quest:the_fremennik_exiles] "Ask about V's Shield." and buying it back
    private suspend fun Dialogue.fremennik() {
        val options = mutableListOf<Pair<String, Topic>>()
        if (QuestRequirements.hasCompleted(player, MOUNTAIN_DAUGHTER)) {
            options += "Ask about the mountain camp." to Topic.MountainCamp
        }
        if (QuestRequirements.hasNotCompleted(player, FREMENNIK_EXILES)) {
            options += "Ask for a quest." to Topic.Quests
        }
        options += "Ask about anything else." to Topic.AnythingElse
        when (choose(options)) {
            Topic.MountainCamp -> mountainCamp()
            Topic.Quests -> exilesOffer()
            else -> anythingElse()
        }
    }

    private suspend fun Dialogue.mountainCamp() {
        chatPlayer(quiz, "So how did the meeting go?")
        chatNpc(
            happy,
            "It went surprisingly well! We are at least at the point where we will freely " +
                "acknowledge each other's existence now.",
        )
        chatNpc(happy, "You are definitely a great asset to the Fremennik, ${fremennikName()}!")
    }

    // TODO(content): [quest:the_fremennik_exiles] quest start: the "Yes." and "No." replies
    private suspend fun Dialogue.exilesOffer() {
        chatPlayer(quiz, "Do you have any quests?")
        chatNpc(
            neutral,
            "As a matter of fact ${fremennikName()}, I do have a quest for you. Are you " +
                "interested?",
        )
    }

    // TODO(content): [system:fremennik_name] "(Charm) Can I have a new name?" while wearing a
    //  ring of Charos(a)
    private suspend fun Dialogue.anythingElse() {
        val sibling = brotherOrSister()
        chatNpc(
            happy,
            "Hello again $sibling ${fremennikName()}! I hope your travels have brought you " +
                "wealth and joy! What compels you to visit me on this day?",
        )
        val option =
            choice4(
                "I just came to say hello.",
                1,
                "Do you have any quests?",
                2,
                "Nice hat!",
                3,
                "Can I hear the history of your people?",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I just came to say hello.")
                chatNpc(
                    happy,
                    "Well met, $sibling ${fremennikName()}. It is always good to see your face " +
                        "in glorious Rellekka once more!",
                )
            }
            2 -> {
                chatPlayer(quiz, "Do you have any quests?")
                chatNpc(
                    neutral,
                    "Not at the moment ${fremennikName()}. Rest assured, should your services " +
                        "to the Fremennik be required, I will call upon you.",
                )
            }
            3 -> {
                chatPlayer(happy, "Nice hat!")
                chatNpc(
                    happy,
                    "Aye, that it is $sibling ${fremennikName()}. Should you desire one of your " +
                        "own, you should go to Skulgrimen's shop and see what they have " +
                        "available!",
                )
            }
            4 -> history()
        }
    }

    private suspend fun Dialogue.history() {
        chatPlayer(quiz, "Can I hear the history of your people?")
        chatNpc(
            happy,
            "Why, of course ${fremennikName()}! Do not say 'your people' like that, for you are " +
                "now a Fremennik yourself! What did you want to hear of?",
        )
        while (true) {
            val option =
                choice4(
                    "Tell me of the finding of Koschei",
                    1,
                    "Tell me of the lands to the North",
                    2,
                    "Tell me of the outerlanders",
                    3,
                    "Don't tell me anything",
                    4,
                )
            when (option) {
                1 -> koschei()
                2 -> landsToTheNorth()
                3 -> outerlanderHistory()
                4 -> {
                    chatPlayer(neutral, "Actually, history isn't really my thing.")
                    chatNpc(
                        neutral,
                        "Well let me know should you wish to hear our lore and histories. We " +
                            "value them highly.",
                    )
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.koschei() {
        chatPlayer(
            quiz,
            "I'm interested in finding out about that mysterious warrior I fought as part of " +
                "Thorvalds' combat trial. His name is Koschei I believe.",
        )
        chatNpc(
            neutral,
            "Ah... the deathless one... We do not even know if Koschei is his name, when we " +
                "found him he had no memory, he was simply repeating that single word.",
        )
        chatNpc(
            neutral,
            "Whatever happened to him must have rattled his soul so hard that his memory ran " +
                "away from him, and he has not yet found it's hiding place.",
        )
        chatPlayer(quiz, "So how exactly did you find him?")
        chatNpc(
            neutral,
            "Well, it was a raiding party like any other. We had just struck one of the smaller " +
                "northern islands to harvest supplies by combat, as our laws allow,",
        )
        chatNpc(
            neutral,
            "when we saw a figure in the icey waters. As we were at least a 3 week sail from " +
                "any port, and saw no other ships nearby, he must have been there some time.",
        )
        chatNpc(
            neutral,
            "We could not leave someone to the cold death of the ocean, even an outerlander, so " +
                "we fished him aboard. He muttered continually, but his words made no sense.",
        )
        chatNpc(
            neutral,
            "He had no clothes or possessions with him, and seemed to have been burnt somehow " +
                "before he had landed in the water.",
        )
        chatNpc(
            neutral,
            "I cannot help but wonder what burnt him, for as you may have seen, he is " +
                "incredibly powerful and resistant to damage by any means I know of!",
        )
        chatNpc(
            neutral,
            "Anyway, we decided to bring him back to Rellekka, for he did not seem of good " +
                "health or sound mind, and simply repeated the word Koschei over and over.",
        )
        chatNpc(
            neutral,
            "Yrsa tended to him for many weeks, until one day it was as though a new soul had " +
                "entered his burnt and broken body.",
        )
        chatNpc(
            neutral,
            "He suddenly left her house one day, fully healed, and able to speak to us as " +
                "though nothing had happened to him.",
        )
        chatNpc(
            neutral,
            "He still had no recollection of how he had come to be in the icey ocean, or of who " +
                "he was, or where he had come from, but this did not matter to us or him.",
        )
        chatNpc(
            neutral,
            "He took his trials of adulthood, as you did, and has been a boon to our clan ever " +
                "since. Someday I fear he may regain his memory, and leave us for his past...",
        )
        chatNpc(
            neutral,
            "But I will always respect any decision he decides to make should that day ever " +
                "come upon us. Until then, he is more than welcome to stay here.",
        )
        chatNpc(
            quiz,
            "So, ${fremennikName()}. Was there anything more you wished to hear?",
        )
    }

    private suspend fun Dialogue.landsToTheNorth() {
        chatPlayer(quiz, "I would like to hear of the lands across the ocean to the North.")
        chatNpc(
            neutral,
            "Well, I think the best thing for you would be to go to the docks and ask to join " +
                "one of our longboats on a journey.",
        )
        chatNpc(
            neutral,
            "My affairs are concentrated mostly on Rellekka, so my knowledge is not great, but " +
                "I will happily share what I do know with you ${fremennikName()}.",
        )
        chatNpc(
            neutral,
            "North of this town the atmosphere becomes bitterly cold. There are a number of " +
                "small inhabited islands that we have sea routes with.",
        )
        chatNpc(
            neutral,
            "The largest of these is possibly Miscellania, although I hear that there has been " +
                "some recent incident with their monarch, but I don't know the details.",
        )
        chatNpc(
            neutral,
            "There is also of course the island of the moon clan, who change with the tides. " +
                "They are our bitterest enemies, and we are currently at war with them.",
        )
        chatNpc(
            neutral,
            "They are an evil people, equally cursed and blessed by the magic in their blood. " +
                "Beyond those islands, there is the large frozen wasteland we call Acheron.",
        )
        chatNpc(
            neutral,
            "It is an inhospitable land, and you will need an agile and sturdy body just to " +
                "keep alive in it's perils.",
        )
        chatNpc(
            quiz,
            "As I say, my knowledge outside of this town is rather limited, and I know no more " +
                "than this. Was there anything else you wished to hear tell of?",
        )
    }

    private suspend fun Dialogue.outerlanderHistory() {
        chatPlayer(
            quiz,
            "I would like to hear a little of the history between the Fremenniks and the " +
                "outerlanders.",
        )
        chatNpc(
            neutral,
            "Ah, now that is something I know a great deal about. Believe it or not, all " +
                "outerlanders were once orginally Fremenniks.",
        )
        chatNpc(
            neutral,
            "When first man came to these lands all were Fremenniks, and followed our ways. We " +
                "lived a happy life, and never settled in one place for long.",
        )
        chatNpc(
            neutral,
            "We travelled by boat along the coastlines, never taking more from the land than " +
                "could be regrown by the same time in the following year.",
        )
        chatNpc(
            neutral,
            "However, many moons past, some of our tribesmen were weary of constantly " +
                "travelling the lands, and decided to build themselves permanent homes.",
        )
        chatNpc(
            neutral,
            "Unfortunately, the other races that had also migrated here did not like this. They " +
                "waged war against us at every opportunity.",
        )
        chatNpc(
            neutral,
            "We were driven mostly to the coastlines, where we could escape by boat when " +
                "attacked. Remember, at this time we had no real way to defend against constant " +
                "attack.",
        )
        chatNpc(
            neutral,
            "Then one day one of our Seers, whose name has been lost in the telling of this " +
                "tale, discovered a cave full of strange rocks.",
        )
        chatNpc(
            neutral,
            "This rock would never be consumed, no matter how much we took, and sparkled at its " +
                "peak, and broke off in small and regular sized chunks when mined.",
        )
        chatNpc(
            neutral,
            "Stranger than this, we found that these rocks would become filled with power when " +
                "taken to certain places across this land!",
        )
        chatNpc(
            neutral,
            "There was a terrible argument in our tribe! Some said that we should take as many " +
                "of these rocks as we could, and use them in defending against our enemies " +
                "attacks!",
        )
        chatNpc(
            neutral,
            "Others said that we had found a place that belonged only to the gods, and that " +
                "should we steal what was not ours we would find only torment and misery.",
        )
        chatNpc(
            neutral,
            "We voted on this, and decided that it was in the best interests of the tribe to " +
                "leave these strange rocks where they lay, for the gods can be spiteful and " +
                "cruel,",
        )
        chatNpc(
            neutral,
            "especially to those who do not treat them with the respect they desire. Some of " +
                "our number refused to accept this ruling by our council however.",
        )
        chatNpc(
            neutral,
            "They began to mine these rocks, and set up transport systems to the various places " +
                "of power that enchanted them.",
        )
        chatNpc(
            angry,
            "They even created temples at each of these places! This was going too far! We had " +
                "no choice, but to expel them from our tribe forever!",
        )
        chatNpc(
            neutral,
            "We turned our backs upon them, and let them know they would never be welcome back " +
                "to our tribes until they had released themselves from the rocks.",
        )
        chatNpc(
            neutral,
            "This is the tale of how the outerlanders came about; through stealing from the " +
                "gods, and from betraying our ideals. This is why we show them no trust.",
        )
        chatNpc(quiz, "Was there something else you wished to hear?")
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
        const val FREMENNIK_EXILES = "quest_fremennikexiles"
        const val MOUNTAIN_DAUGHTER = "quest_mountaindaughter"
    }

    private enum class Topic {
        MountainCamp,
        AnythingElse,
        Place,
        Quests,
        Hat,
        NoOneTalks,
        Outerlander,
        Army,
        Fremennik,
    }
}
