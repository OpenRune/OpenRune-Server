package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:song_of_the_elves] after-quest dialogue for the West Ardougne citizens
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
    // TODO(content): [quest:plague_city] hangover cure, warrant and after-quest dialogue
    private suspend fun Dialogue.bravek() {
        chatPlayer(happy, "Hey Bravek.")
        chatNpc(
            neutral,
            "Hello there. Can't stop to talk I'm afraid, I've been left with a lot of work to do " +
                "thanks to those mourners.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Carla (before Song of the Elves)
    private suspend fun Dialogue.carla() {
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

    // https://oldschool.runescape.wiki/w/Transcript:Clerk (before Plague City)
    // TODO(content): [quest:plague_city] letting the player see Bravek
    private suspend fun Dialogue.clerk() {
        chatNpc(happy, "Hello, welcome to the Civic Office of West Ardougne. How can I help you?")
        if (!choice2("Who is through that door?", true, "I'm just looking thanks.", false)) {
            chatPlayer(neutral, "I'm just looking thanks.")
            return
        }
        chatPlayer(quiz, "Who is through that door?")
        chatNpc(neutral, "The city warder Bravek is in there.")
        chatPlayer(quiz, "Can I go in?")
        chatNpc(neutral, "He has asked not to be disturbed.")
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

    // https://oldschool.runescape.wiki/w/Transcript:Jethick (before Plague City)
    // TODO(content): [quest:plague_city] quest stages and after-quest dialogue
    // TODO(content): [system:treasure_trails] medium clue challenge scroll
    private suspend fun Dialogue.jethick() {
        chatNpc(neutral, "Hello, I don't recognise you. We don't get many newcomers around here.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kilron (before Plague City)
    // TODO(content): [quest:biohazard] quest stages
    private suspend fun Dialogue.kilron() {
        chatPlayer(happy, "Hello there.")
        chatNpc(neutral, "Hello.")
        chatPlayer(quiz, "How are you?")
        chatNpc(neutral, "Busy.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nurse_Sarah
    // TODO(content): [quest:biohazard] dialogue during Biohazard
    private suspend fun Dialogue.nurseSarah() {
        chatPlayer(happy, "Hello there.")
        chatNpc(happy, "Hello my dear, how are you feeling?")
        chatPlayer(happy, "I'm okay thanks.")
        chatNpc(happy, "Well in that case I'd better get back to work. Take care.")
        chatPlayer(happy, "You too.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Priest_(West_Ardougne)
    private suspend fun Dialogue.priest() {
        chatPlayer(happy, "Hello there.")
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

    // https://oldschool.runescape.wiki/w/Transcript:Ted_Rehnison (before Plague City)
    // TODO(content): [quest:plague_city] quest stages and after-quest dialogue
    private suspend fun Dialogue.tedRehnison() {
        chatNpc(angry, "Go away. We don't want any.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mourner_(Plague_City) (West Ardougne)
    // TODO(content): [quest:plague_city] "I'm looking for a woman named Elena." during the quest
    // TODO(content): [quest:plague_city] after-quest dialogue
    private suspend fun Dialogue.mourner() {
        chatNpc(
            neutral,
            "Hmmm, how did you get over here? You're not one of this rabble. Ah well, you'll " +
                "have to stay. Can't risk you going back now.",
        )
        whatsAMourner()
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
        if (!whatsAMourner) {
            chatPlayer(neutral, "I haven't got the plague though...")
            chatNpc(
                neutral,
                "Can't risk you being a carrier. That protective clothing you have isn't " +
                    "regulation issue. It won't meet safety standards.",
            )
            return
        }
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
    }
}
