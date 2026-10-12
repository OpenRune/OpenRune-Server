package org.rsmod.content.areas.misc.islands.lunarisle.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LunarPirates : PluginScript() {
    override fun ScriptContext.startup() {
        for (pirate in GENERIC_PIRATES) {
            onOpNpc1(pirate) { startDialogue(it.npc) { genericPirate() } }
        }
        onOpNpc1("npc.lunar_pirate_cook") { startDialogue(it.npc) { beefyBurns() } }
        onOpNpc1("npc.lunar_pirate_first_mate") { startDialogue(it.npc) { daveyBoy() } }
        onOpNpc1("npc.lunar_pirate_lookout") { startDialogue(it.npc) { eagleEyeShultz() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Picarron%27_Pete
    private suspend fun Dialogue.genericPirate() {
        val male = access.isBodyTypeA()
        chatPlayer(happy, "Hello.")
        when (access.random.of(10)) {
            0 -> {
                chatPlayer(quiz, "So how's life as a pirate?")
                chatNpc(
                    confused,
                    "What kind of question is that? How's life as a... I dunno. Whatever it is " +
                        "that you do for a living.",
                )
                chatPlayer(happy, "I'm a freelance troubleshooter.")
                chatNpc(quiz, "What does that entail then?")
                chatPlayer(
                    neutral,
                    "Mostly killing things for money and delivering items around the planet " +
                        "for people.",
                )
                chatPlayer(neutral, "I collect stuff.")
                chatNpc(quiz, "So how's that life?")
                chatPlayer(happy, "Can't complain, can't complain...")
                chatNpc(neutral, "Well, there you go.")
            }
            1 -> {
                chatPlayer(quiz, "Aren't you a little short for a pirate?")
                chatNpc(neutral, "My mother was a gnome. Apparently it was a very painful birth.")
                chatPlayer(shocked, "More info than I wanted, thanks!")
            }
            2 -> {
                chatPlayer(
                    quiz,
                    "You know, I've always wondered what life as a pirate actually entails.",
                )
                chatNpc(
                    neutral,
                    "Well, at the moment it mostly involves being asked random questions by a " +
                        "stowaway.",
                )
                chatPlayer(
                    angry,
                    "I'm not a stowaway! I was invited aboard! By Lokar! Ask him!",
                )
                chatNpc(
                    neutral,
                    "Hey, whatever ${if (male) "pal" else "love"}. Just make sure the captain " +
                        "doesn't catch you, pirates don't like stowaways much.",
                )
            }
            3 -> {
                chatNpc(
                    happy,
                    "Ah, good day to you ${if (male) "sirrah" else "milady"}! Your face is " +
                        "unfamiliar, did you perhaps join us aboard the ship at Lunar Isle?",
                )
                chatPlayer(neutral, "No, Lokar offered me a lift in Rellekka actually.")
                chatNpc(quiz, "Oh, really? You don't look like a Fremennik to me!")
                chatPlayer(
                    neutral,
                    "Well... I kind of am, and I kind of aren't. It's a long story.",
                )
                chatNpc(
                    happy,
                    "Sorry I don't have time to hear it then! See you around young " +
                        "fremennik-who-is-not-really-a-fremennik!",
                )
                chatPlayer(neutral, "'Bye.")
            }
            4 -> {
                chatPlayer(worried, "Brrrr! Its cold up here!")
                chatNpc(
                    neutral,
                    "You think this is cold? Up by Acheron it gets so cold that when you talk " +
                        "you see the words freeze in the air in front of you!",
                )
                chatPlayer(shocked, "REALLY?")
                chatNpc(
                    laugh,
                    "Nah, not really. I was exaggerating for humourous effect. It is very very " +
                        "cold though!",
                )
            }
            5 -> {
                chatNpc(happy, "Hello to you too.")
                chatPlayer(
                    happy,
                    "Yar! We be pirates, yar! Avast, ye scurvy land-lubbing lychee!",
                )
                chatNpc(angry, "Please don't talk like that, it is extremely irritating.")
                chatNpc(angry, "Also, please don't call me a lychee, whatever that may be.")
                chatPlayer(sad, "Oh. Okay. Sorry.")
            }
            6 -> {
                chatNpc(shocked, "ARGH! SOUND THE ALARM! STOWAWAY ON BOARD! STOWAWAY ON BOARD!")
                chatPlayer(
                    shocked,
                    "No! I'm not a stowaway! Honest! I was invited here!",
                )
                chatNpc(neutral, "Oh, sorry, my mistake then.")
                chatNpc(neutral, "You must admit you do look a lot like a stowaway though.")
                chatPlayer(quiz, "Why, what do they usually look like?")
                chatNpc(confused, "Erm... I've never actually met one...")
                chatPlayer(neutral, "Okay then...")
            }
            7 -> {
                chatNpc(happy, "Hello.")
                chatPlayer(quiz, "So... You're a pirate, huh?")
                chatNpc(neutral, "It's what it says on my pay-packet at the end of the month.")
                chatPlayer(quiz, "How's that working out for you?")
                chatNpc(
                    happy,
                    "Pretty good so far. All the grog and loot that we can plunder, plus full " +
                        "medical including dental.",
                )
                chatPlayer(quiz, "You mean you have insurance?")
                chatNpc(
                    neutral,
                    "Not as such. If any of us get sick we kidnap a doctor and don't let him go " +
                        "until we're better.",
                )
                chatNpc(
                    neutral,
                    "You'd be surprised what an incentive for expert health care that is.",
                )
                chatPlayer(neutral, "I can imagine.")
            }
            8 -> {
                chatNpc(happy, "Hello there. So what brings you aboard the Lady Zay?")
                chatPlayer(
                    happy,
                    "Well, I was planning on visiting the Moon Clan, but I have to say your " +
                        "ship is very impressive.",
                )
                chatNpc(
                    happy,
                    "Aye, she's a beauty alright! The Lady Zay has been my home for many hard " +
                        "months, through storm and sun, and she always gets us to here we were " +
                        "headed!",
                )
                chatPlayer(
                    happy,
                    "Yes, she's certainly one of the finest boats I've seen on my travels!",
                )
                chatNpc(happy, "That she is ${if (male) "lad" else "lass"}, that she is.")
            }
            else -> {
                chatNpc(
                    worried,
                    "Sorry, can't stop, the Captain will have my guts for garters if he catches " +
                        "me slacking off talking to the stowaway.",
                )
                chatPlayer(angry, "I'm not a stowaway! I was invited aboard!")
                chatNpc(
                    worried,
                    "Yeah, whatever ${if (male) "guy" else "lady"}, it doesn't really matter " +
                        "who you are I'll get in trouble!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Beefy%27_Burns
    private suspend fun Dialogue.beefyBurns() {
        chatPlayer(quiz, "What are you cooking?")
        chatNpc(happy, "My speciality! What else could I be cooking?")
        chatPlayer(quiz, "Ok, and your speciality is...?")
        chatNpc(happy, "Boiled shark guts with a hint of rosemary and a dash of squid ink.")
        chatPlayer(worried, "I think I'll stick to making my own food.")
        chatNpc(neutral, "Your loss!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:First_mate_%27Davey-boy%27
    private suspend fun Dialogue.daveyBoy() {
        chatPlayer(quiz, "What does it take to become first mate on a ship?")
        chatNpc(
            neutral,
            "Good question. We have a diplomatic consession at the turn of the financial year. " +
                "Said pirate is chosen should the existing mate be absent without leave.",
        )
        chatPlayer(neutral, "I had no idea. I always figured it was all about popularity.")
        chatNpc(laugh, "It is. I'm just pulling your leg.")
        chatPlayer(sad, "Oh....")
    }

    // https://oldschool.runescape.wiki/w/Transcript:%27Eagle-eye%27_Shultz
    private suspend fun Dialogue.eagleEyeShultz() {
        chatPlayer(
            quiz,
            "What do you do for fun on this ship? You know, when you're not doing pirate stuff.",
        )
        chatNpc(happy, "All sorts! Hide and seek, pin the patch on the pirate, walk the plank!")
        chatPlayer(
            shocked,
            "What a life! Wait a minute. 'Walk the plank'? Surely that's a bit dangerous?",
        )
        chatNpc(laugh, "Well of course, but where's the fun without a few deaths?")
        chatPlayer(worried, "I think I'll stick to Runelink.")
    }

    private companion object {
        val GENERIC_PIRATES = (1..10).map { "npc.lunar_pirate_generic_pirate_$it" }
    }
}
