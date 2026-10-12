package org.rsmod.content.areas.misc.lobbies.nightmarezone.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Dominic_Onion (no dream prepared)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:nightmare_zone] dream selection after "Can I choose a dream?", the
//  prepared-dream greeting, "I want to change my dream" and the Dream option
// TODO(content): [system:treasure_trails] medium and elite clue steps
class DominicOnion : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.nzone_host") { startDialogue(it.npc) { dominic() } }
    }

    private suspend fun Dialogue.dominic() {
        chatNpc(
            happy,
            "Welcome to the Nightmare Zone! Would you like me to create a dream for you?",
        )
        while (true) {
            val option =
                choice5(
                    "Who are you?",
                    1,
                    "What is this place?",
                    2,
                    "Can you tell me about the dreams?",
                    3,
                    "Can I choose a dream?",
                    4,
                    "I'll come back another time.",
                    5,
                )
            when (option) {
                1 -> whoAreYou()
                2 -> whatIsThisPlace()
                3 -> aboutTheDreams()
                4 -> {
                    chatPlayer(quiz, "Can I choose a dream?")
                    chatNpc(
                        happy,
                        "Certainly. You can practice for free, but the larger dreams require " +
                            "more skill to imbue the potions and so there's a fee for those. " +
                            "Which dream would you like to experience?",
                    )
                    return
                }
                5 -> {
                    chatPlayer(neutral, "I'll come back another time.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.whoAreYou() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(happy, "My name's Dominic Onion but you can call me Dom, or Mr Onion.")
        chatPlayer(confused, "Mr Oni...Dom, how did you get that name?")
        chatNpc(
            laugh,
            "Well that's a long story...but I can tell you that it involved my parents, 3 " +
                "blurberry specials, and a chinchompa.",
        )
        chatPlayer(confused, "...and a chinchompa?")
        chatNpc(
            laugh,
            "Oh yes, Nigel played a crucial role in the whole debacle. It's all very amusing now " +
                "that I think back to it.",
        )
        chatPlayer(neutral, "Right...")
        chatNpc(happy, "Good times...")
    }

    private suspend fun Dialogue.whatIsThisPlace() {
        chatPlayer(quiz, "What is this place?")
        chatNpc(happy, "This is the Nightmare Zone!")
        chatPlayer(shocked, "Wwwwhat?!")
        chatNpc(happy, "Don't worry, it's not as bad as it sounds...most of the time.")
        chatPlayer(worried, "That's comforting.")
        chatNpc(
            neutral,
            "Truth be told, that name wasn't my first choice, it's what the local folk have been " +
                "calling it and so it sort of stuck.",
        )
        chatNpc(
            neutral,
            "My original plan came to me in a vivid dream when I was younger you see, I saw " +
                "myself running a successful business from a great structure in a strange land.",
        )
        chatNpc(
            neutral,
            "Well, I say strange, but I'd never left Lunar Isle at the time and this place was " +
                "very hot and dry, unlike anything I'd seen before.",
        )
        chatNpc(
            neutral,
            "When I told my father about the dream he laughed and said I should become a banker " +
                "like him and his father before him but in that moment I knew that it wasn't " +
                "the life for me.",
        )
        chatNpc(
            neutral,
            "I had to find out what this dream meant and if it was a vision of the future or not.",
        )
        chatNpc(
            neutral,
            "Growing up, I studied hard to find out more about magic, especially oneiromancy and " +
                "the interpretation of dreams.",
        )
        chatNpc(
            neutral,
            "Having learned as much as I could on Lunar Isle I set out to find and learn from " +
                "other great wizards I'd heard of in stories as a child.",
        )
        chatNpc(
            neutral,
            "I set off to travel the world, discovering more about dreams to enable me to " +
                "understand the dream I'd had as a child.",
        )
        chatNpc(
            neutral,
            "I needed to know what this dream meant and if it was truly a vision of the future.",
        )
        chatNpc(
            neutral,
            "If so I knew I had to try and find this location and build the huge tower I saw.",
        )
        chatNpc(
            neutral,
            "I was going to call it 'Dom Onion's Tower', but I've not found this strange place " +
                "or gathered enough money to build it just yet.",
        )
        chatNpc(
            happy,
            "That's why I've setup this small business venture in the mean time selling dreams " +
                "to people. I just need a big enough crowd to kickstart my business.",
        )
        chatNpc(
            happy,
            "It's all part of my five year plan, and who knows, perhaps my dream will come " +
                "true; I'll discover this place and build Dom Onion's Tower!",
        )
    }

    // TODO(content): [verify:in_game] the terms and conditions line is shown as a mesbox
    private suspend fun Dialogue.aboutTheDreams() {
        chatPlayer(quiz, "Can you tell me about the dreams?")
        chatNpc(
            happy,
            "Certainly. I can delve into your memories and search for the key events that have " +
                "affected you on your adventures.",
        )
        chatNpc(
            neutral,
            "I then take the essence of these memories and put that in the vial you see on the " +
                "plinth.",
        )
        chatNpc(
            neutral,
            "I imbue a bit of magic, then give the vial a bit of a shake, and when you drink it " +
                "you'll be able to re-live an encounter close to the original.",
        )
        chatNpc(
            neutral,
            "Don't worry though, when you're in a dream, you're perfectly safe even if it does " +
                "seem very real.",
        )
        chatNpc(
            neutral,
            "If you sucessfully overcome the dream or die trying, you'll leave the dream state " +
                "and return to normal with your grey matter and appendages intact.",
        )
        chatPlayer(quiz, "Are all dreams the same or are there different options?")
        chatNpc(
            happy,
            "Oh no, they can vary wildly! You can choose from one of three main options.",
        )
        chatNpc(
            neutral,
            "Firstly, I can create a one-on-one encounter with an adversary you've defeated in " +
                "your adventures. People like to practice in these dreams.",
        )
        chatNpc(
            neutral,
            "Then there's a sort of Endurance dream, where you face enemies in encounter after " +
                "encounter, and try to survive to the end.",
        )
        chatNpc(
            happy,
            "And finally there's something I like to call a Rumble, where you face multiple " +
                "adversaries at the same time. I imbue a bit more magic and shake the vial " +
                "harder to create that one!",
        )
        chatNpc(
            neutral,
            "You can invite friends to fight alongside you in a Rumble dream. Once I've set up " +
                "the dream for you, step into the enclosure and invite up to 4 friends.",
        )
        chatNpc(
            neutral,
            "For the Endurance and Rumble dreams, I charge a fee for my work. I don't like " +
                "handling the money directly, so instead I've set up a secure coffer.",
        )
        chatNpc(
            neutral,
            "After you've asked me to set up a dream, put some money in my coffer. I'll deduct " +
                "the fee from the coffer when you start the dream.",
        )
        chatNpc(
            neutral,
            "There's a red vial on the other plinth. Your friends can drink from it to enter " +
                "your dream as a spectator to observe your progress.",
        )
        chatNpc(
            neutral,
            "Finally here's one other option you get, and that's how difficult the encounters " +
                "are. As well as the normal dreams, the bravest adventurers can choose a harder " +
                "version*.",
        )
        mesbox(
            "*Terms and conditions apply: Mr Onion, henceforth known as Dom, cannot be held " +
                "liable for accidental loss of limbs (yours or otherwise) and or psychological " +
                "damage as a result of entering the Nightmare Zone. Please note there is a " +
                "strict no-refunds policy, and all dream purchases are final."
        )
    }
}
