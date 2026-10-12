package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] Billy Rehnison's line is a game message, not a mesbox
class WestArdougneTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.bravek") { startDialogue(it.npc) { bravek() } }
        onOpNpc1("npc.carla") { startDialogue(it.npc) { carla() } }
        onOpNpc1("npc.clerk") { startDialogue(it.npc) { clerk() } }
        onOpNpc1("npc.clerk2") { startDialogue(it.npc) { easternClerk() } }
        onOpNpc1("npc.jethick") { startDialogue(it.npc) { jethick() } }
        onOpNpc1("npc.kilron") { startDialogue(it.npc) { kilron() } }
        onOpNpc1("npc.bionurse") { startDialogue(it.npc) { nurseSarah() } }
        onOpNpc1("npc.w_ardoungepriest") { startDialogue(it.npc) { priest() } }
        onOpNpc1("npc.recruiter") { startDialogue(it.npc) { recruiter() } }
        onOpNpc1("npc.recruiter_vis") { startDialogue(it.npc) { recruiter() } }
        onOpNpc1("npc.ted_rehnison") { startDialogue(it.npc) { tedRehnison() } }
        onOpNpc1("npc.billy_rehnison") { mes("Billy isn't interested in talking.") }
        onOpNpc1("npc.mourner1") { startDialogue(it.npc) { mourner() } }
        onOpNpc1("npc.mourner2") { startDialogue(it.npc) { mourner() } }
        onOpNpc1("npc.mourner3") { startDialogue(it.npc) { mourner() } }
        onOpNpc1("npc.headmourner") { startDialogue(it.npc) { headMourner() } }
        onOpNpc1("npc.headmourner_vis") { startDialogue(it.npc) { headMourner() } }
        onOpNpc1("npc.upassmage") { startDialogue(it.npc) { darkMage() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bravek
    // TODO(content): [quest:plague_city] hangover cure and warrant
    private suspend fun Dialogue.bravek() {
        if (QuestRequirements.hasCompleted(player, PLAGUE_CITY)) {
            chatNpc(happy, "Thanks again for the hangover cure.")
            chatPlayer(happy, "Not a problem, happy to help out.")
            chatNpc(
                drunk,
                "I'm just having a little drop of whisky, then I'll feel really good.",
            )
            return
        }
        chatPlayer(happy, "Hey Bravek.")
        chatNpc(
            neutral,
            "Hello there. Can't stop to talk I'm afraid, I've been left with a lot of work to do " +
                "thanks to those mourners.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Carla
    private suspend fun Dialogue.carla() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatPlayer(happy, "Hey Carla, how are you doing?")
            chatNpc(
                sad,
                "I'm okay. The loss of my son is something that I'll never be over. At least no " +
                    "one else has to suffer at the hands of those mourners though.",
            )
            return
        }
        chatPlayer(happy, "Hello.")
        chatNpc(sad, "Oh, hello.")
        chatPlayer(worried, "You seem upset... What's wrong?")
        chatNpc(sad, "My son fell ill with the plague...")
        chatPlayer(sad, "That's awful, I'm sorry.")
        chatNpc(
            sad,
            "It would be easier to cope with if I could have spent his last few days with him.",
        )
        chatPlayer(quiz, "Why didn't you?")
        chatNpc(
            sad,
            "Those mourners came and whisked him away. He didn't even seem that ill, I thought " +
                "it was a common cold... But the mourners said that he was infected and had to " +
                "be taken away.",
        )
        chatNpc(sad, "Two days later the mourners returned and told me he had died.")
        chatPlayer(sad, "Again, I'm sorry. Life can be harsh.")
        val option =
            choice3(
                "Where did the plague come from?",
                1,
                "Have there been many victims of the plague?",
                2,
                "I hope things get easier for you.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Where did the plague come from?")
                chatNpc(
                    angry,
                    "It's down to King Tyras. He and his men brought the plague here from the " +
                        "west, and then left us to suffer. One day he'll pay for what he's done!",
                )
            }
            2 -> {
                chatPlayer(quiz, "Have there been many victims of the plague?")
                chatNpc(
                    sad,
                    "You could say that... I've heard reports that half of West Ardougne is " +
                        "infected! Many have lost friends and family...",
                )
                chatPlayer(sad, "It sounds an awful way to live.")
                chatNpc(
                    sad,
                    "People are very depressed and scared. I've never met anyone fully infected " +
                        "though. I suppose we should be grateful to the mourners for that.",
                )
            }
            3 -> {
                chatPlayer(sad, "I hope things get easier for you.")
                chatNpc(sad, "Me too...")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Clerk
    private suspend fun Dialogue.clerk() {
        chatNpc(happy, "Hello, welcome to the Civic Office of West Ardougne. How can I help you?")
        if (!choice2("Who is through that door?", true, "I'm just looking thanks.", false)) {
            chatPlayer(neutral, "I'm just looking thanks.")
            return
        }
        chatPlayer(quiz, "Who is through that door?")
        chatNpc(neutral, "The city warder Bravek is in there.")
        chatPlayer(quiz, "Can I go in?")
        if (QuestRequirements.hasCompleted(player, PLAGUE_CITY)) {
            chatNpc(neutral, "I suppose so.")
        } else {
            chatNpc(neutral, "He has asked not to be disturbed.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Clerk (clerk in eastern room)
    // TODO(content): [verify:in_game] npc.clerk2 is the clerk in the eastern room
    private suspend fun Dialogue.easternClerk() {
        chatPlayer(happy, "Hello.")
        chatNpc(
            neutral,
            "Sorry, I don't have time to talk. Bravek needs lots of help running the city.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jethick
    // TODO(content): [quest:plague_city] quest stages
    // TODO(content): [quest:song_of_the_elves] the greeting from when the mourners leave during
    //  the quest
    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.jethick() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(happy, "Good to see you friend.")
            chatPlayer(quiz, "Hey Jethick, how's things?")
            chatNpc(
                neutral,
                "Not bad, the city is still a mess but we've finally got the chance to change " +
                    "that now the mourners have gone.",
            )
            chatPlayer(happy, "Good to hear, all the best.")
            return
        }
        if (QuestRequirements.hasCompleted(player, PLAGUE_CITY)) {
            chatNpc(
                neutral,
                "I'm surprised you're still here. Not many like to stick around this place if " +
                    "they have the choice.",
            )
            return
        }
        chatNpc(neutral, "Hello, I don't recognise you. We don't get many newcomers around here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kilron
    // TODO(content): [quest:biohazard] quest stages
    private suspend fun Dialogue.kilron() {
        chatPlayer(happy, "Hello there.")
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(happy, "Hello friend.")
            return
        }
        chatNpc(neutral, "Hello.")
        chatPlayer(quiz, "How are you?")
        chatNpc(neutral, "Busy.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nurse_Sarah
    private suspend fun Dialogue.nurseSarah() {
        if (QuestRequirements.isOnQuest(player, BIOHAZARD)) {
            chatPlayer(happy, "Hello nurse.")
            chatNpc(sad, "I don't know how much longer I can cope here.")
            chatPlayer(quiz, "What? Is the plague getting to you?")
            chatNpc(
                neutral,
                "No, strangely enough the people here don't seem to be affected. It's just the " +
                    "awful living conditions that is making people ill.",
            )
            chatPlayer(confused, "I was under the impression that everyone here was affected.")
            chatNpc(neutral, "Me too, but that doesn't seem to be the case.")
            return
        }
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello my dear, how are you feeling?")
        chatPlayer(happy, "I'm okay thanks.")
        chatNpc(happy, "Well in that case I'd better get back to work. Take care.")
        chatPlayer(happy, "You too.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Priest_(West_Ardougne)
    private suspend fun Dialogue.priest() {
        chatPlayer(happy, "Hello there.")
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(
                neutral,
                "It's going to take a lot of work to fix the damage done to this city.",
            )
            return
        }
        chatNpc(sad, "I wish there was more I could do for these people.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Recruiter
    // TODO(content): [verify:in_game] which men heckle the recruiter and the tomato throw
    private suspend fun Dialogue.recruiter() {
        chatNpc(
            happy,
            "Citizens of West Ardougne! King Tyras needs you for his Royal Army! Who will join " +
                "this noble cause?",
        )
        chatNpcSpecific("Man", HECKLER_1, angry, "Plague bringer!")
        chatNpcSpecific("Man", HECKLER_2, angry, "King Tyras is scum!")
        chatNpc(angry, "Tyras will be informed of these words of treason!")
        mesbox("Someone throws a tomato at the recruiter.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ted_Rehnison
    // TODO(content): [quest:plague_city] quest stages
    private suspend fun Dialogue.tedRehnison() {
        if (QuestRequirements.hasCompleted(player, SONG_OF_THE_ELVES)) {
            chatNpc(happy, "Good day friend.")
            return
        }
        if (QuestRequirements.hasCompleted(player, PLAGUE_CITY)) {
            chatNpc(quiz, "Any luck finding Elena yet?")
            chatPlayer(happy, "Yes, she is safe at home now.")
            chatNpc(happy, "That's good to hear, she helped us a lot.")
            return
        }
        chatNpc(angry, "Go away. We don't want any.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mourner_(Plague_City) (West Ardougne)
    // TODO(content): [quest:plague_city] after-quest dialogue (not in the transcript)
    private suspend fun Dialogue.mourner() {
        chatNpc(
            neutral,
            "Hmmm, how did you get over here? You're not one of this rabble. Ah well, you'll " +
                "have to stay. Can't risk you going back now.",
        )
        if (QuestRequirements.isOnQuest(player, PLAGUE_CITY)) {
            whatsAMournerOrElena()
            return
        }
        whatsAMourner()
    }

    private suspend fun Dialogue.whatsAMournerOrElena() {
        val option =
            choice3(
                "So what's a mourner?",
                1,
                "I haven't got the plague though...",
                2,
                "I'm looking for a woman named Elena.",
                3,
            )
        when (option) {
            1 -> soWhatsAMourner()
            2 -> haventGotThePlague()
            3 -> {
                chatPlayer(quiz, "I'm looking for a woman named Elena.")
                chatNpc(
                    neutral,
                    "Ah yes, I've heard of her. A healer I believe. She must be mad coming over " +
                        "here voluntarily.",
                )
                chatNpc(
                    sad,
                    "I hear rumours she has probably caught the plague now. Very tragic, a " +
                        "stupid waste of life.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Head_Mourner
    // TODO(content): [quest:plague_city] quest stages
    private suspend fun Dialogue.headMourner() {
        chatNpc(
            neutral,
            "How did you get into West Ardougne? Ah well you'll have to stay, can't risk you " +
                "spreading the plague outside.",
        )
        whatsAMourner()
    }

    private suspend fun Dialogue.whatsAMourner() {
        val whatsAMourner =
            choice2(
                "So what's a mourner?",
                true,
                "I haven't got the plague though...",
                false,
            )
        if (whatsAMourner) {
            soWhatsAMourner()
        } else {
            haventGotThePlague()
        }
    }

    private suspend fun Dialogue.haventGotThePlague() {
        chatPlayer(neutral, "I haven't got the plague though...")
        chatNpc(
            neutral,
            "Can't risk you being a carrier. That protective clothing you have isn't " +
                "regulation issue. It won't meet safety standards.",
        )
    }

    private suspend fun Dialogue.soWhatsAMourner() {
        chatPlayer(quiz, "So what's a mourner?")
        chatNpc(
            neutral,
            "We're working for King Lathas of East Ardougne. He has tasked us with containing " +
                "the accursed plague sweeping West Ardougne.",
        )
        chatNpc(
            neutral,
            "We also do our best to ease these people's suffering. We're nicknamed mourners " +
                "because we spend a lot of time at plague victim funerals, no one else is " +
                "allowed to risk attending.",
        )
        chatNpc(neutral, "It's a demanding job, and we get little thanks from the people here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Dark_Mage
    // TODO(content): [quest:underground_pass] Staff of Iban recharge, repair and upgrade
    private suspend fun Dialogue.darkMage() {
        chatNpc(
            angry,
            "Why do you interrupt me, traveller? I am experimenting with dark magic; it is a " +
                "dangerous craft, and I prefer not to be disturbed.",
        )
        chatPlayer(neutral, "I'm just looking around.")
        chatNpc(neutral, "There's nothing to see here, just despair and death.")
    }

    private companion object {
        const val HECKLER_1 = "npc.w_ardougnecitizen3"
        const val HECKLER_2 = "npc.w_ardougnecitizen4"

        const val PLAGUE_CITY = "quest_plaguecity"
        const val BIOHAZARD = "quest_biohazard"
        const val SONG_OF_THE_ELVES = "quest_songoftheelves"
    }
}
