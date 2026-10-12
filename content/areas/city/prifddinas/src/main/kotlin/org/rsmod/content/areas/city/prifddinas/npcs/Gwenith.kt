package org.rsmod.content.areas.city.prifddinas.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Gwenith : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gwenith_elf_1") { startDialogue(it.npc) { rhoddwrTan() } }
        onOpNpc1("npc.gwenith_elf_2") { startDialogue(it.npc) { meddwlYmlaenLlaw() } }
        onOpNpc1("npc.gwenith_elf_3") { startDialogue(it.npc) { lleidrGolau() } }
        onOpNpc1("npc.gwenith_elf_4") { startDialogue(it.npc) { gwerinHapus() } }
        onOpNpc1("npc.gwenith_elf_5") { startDialogue(it.npc) { preswylwyrDall() } }
        onOpNpc1("npc.gwenith_elf_6") { startDialogue(it.npc) { marwolaethDawnsio() } }
        onOpNpc1("npc.prif_ed") { startDialogue(it.npc) { ed() } }
        onOpNpc1("npc.prif_ghost") { startDialogue(it.npc) { ghost() } }
        onOpNpc1("npc.prif_lenny") { startDialogue(it.npc) { lenny() } }
        onOpNpc1("npc.prif_tide") { startDialogue(it.npc) { tide() } }
        onOpNpc1("npc.prif_slippers") { startDialogue(it.npc) { musician() } }
        onOpNpc1("npc.prif_surma") { startDialogue(it.npc) { musician() } }
    }

    private suspend fun Dialogue.greet(): Unit = chatPlayer(happy, "Hi.")

    private suspend fun Dialogue.whoAreYou(name: String, intro: String) {
        chatPlayer(quiz, "Who are you?")
        chatNpc(happy, intro)
        chatPlayer(happy, "It's nice to meet you $name, I'm ${player.displayName}.")
        chatNpc(happy, "And it's great to meet you, ${player.displayName}.")
    }

    private suspend fun Dialogue.bye() {
        chatPlayer(happy, "Bye!")
        chatNpc(happy, "Farewell, adventurer.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Rhoddwr_Tan
    private suspend fun Dialogue.rhoddwrTan() {
        greet()
        chatNpc(happy, "Hey! What can I do for ya' today?")
        while (true) {
            when (choice3("Who are you?", 1, "What do you do here?", 2, "Bye!", 3)) {
                1 -> whoAreYou("Rhoddwr", "I'm Rhoddwr, a simple forest dweller.")
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        happy,
                        "I'm the leader of this small camp. We live amongst the trees, at one " +
                            "with the forest.",
                    )
                    chatPlayer(happy, "That sounds relaxing.")
                    chatNpc(
                        happy,
                        "That's because it is! This is where we belong, this is where we began.",
                    )
                    chatPlayer(neutral, "A nice sentiment, but we can't all live off the land.")
                    chatNpc(
                        neutral,
                        "Very true! As we expand and grow, nature refuses to adjust to our ways. " +
                            "The only other option is to destroy it to make room for growth.",
                    )
                    chatPlayer(neutral, "Hopefully nature understands the necessity of it all.")
                    chatNpc(
                        neutral,
                        "History shows again and again that nature points out the folly of man.",
                    )
                    chatPlayer(
                        neutral,
                        "Which is why we should learn from the past and avoid making such " +
                            "mistakes.",
                    )
                    chatNpc(
                        neutral,
                        "Indeed, although some mistakes have lasting effects that cannot be " +
                            "undone. Is there anything else you wanted to know?",
                    )
                }
                else -> return bye()
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Meddwl_Ymlaen_Llaw
    private suspend fun Dialogue.meddwlYmlaenLlaw() {
        greet()
        chatNpc(happy, "Hey! What can I do for ya' today?")
        while (true) {
            when (choice3("Who are you?", 1, "What do you do here?", 2, "Bye!", 3)) {
                1 -> whoAreYou("Meddwl", "I'm Meddwl, a simple forest dweller.")
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(neutral, "I'm new here, I recently left my home of Gwenith.")
                    chatPlayer(quiz, "How come?")
                    chatNpc(
                        neutral,
                        "Gwenith may be in the forest, it is not part of the forest. I desired " +
                            "to be closer to nature.",
                    )
                    chatPlayer(quiz, "A lovely idea, how's it going?")
                    chatNpc(
                        happy,
                        "It's a harder living without the protection of four walls and a roof, " +
                            "but the peace is a wonderful thing to behold. Just listen...",
                    )
                    chatPlayer(silent, "...")
                    chatNpc(silent, "...")
                    chatPlayer(silent, "...")
                    chatNpc(happy, "Isn't it lovely?")
                    chatPlayer(happy, "It is! I feel almost lighter.")
                    chatNpc(
                        happy,
                        "That's your demons flittering away, taken by the cool breeze of nature.",
                    )
                    chatNpc(happy, "Any who, was there anything else you wanted?")
                }
                else -> return bye()
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lleidr_Golau
    private suspend fun Dialogue.lleidrGolau() {
        greet()
        chatNpc(happy, "Hey! What can I do for ya' today?")
        while (true) {
            when (choice3("Who are you?", 1, "What do you do here?", 2, "Bye!", 3)) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(neutral, "I'm Lleidr, a simple forest dweller.")
                    chatPlayer(happy, "It's nice to meet you Lleidr, I'm ${player.displayName}.")
                    chatNpc(neutral, "Great.")
                }
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(neutral, "Currently, I'm fishing.")
                    chatPlayer(quiz, "Anything else?")
                    chatNpc(neutral, "Avoid idle chit-chat.")
                    chatPlayer(neutral, "Alright, I get the hint.")
                    chatNpc(neutral, "That's nice to hear, see you around.")
                    chatPlayer(neutral, "...Bye.")
                    return
                }
                else -> return bye()
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gwerin_Hapus
    private suspend fun Dialogue.gwerinHapus() {
        greet()
        chatNpc(happy, "Hello! What can I do for you today?")
        while (true) {
            when (choice3("Who are you?", 1, "What do you do here?", 2, "Bye!", 3)) {
                1 -> whoAreYou("Gwerin", "I'm Gwerin.")
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        happy,
                        "I'm the village miller. I keep the wheat growing strong and turn it " +
                            "into flour.",
                    )
                    chatPlayer(happy, "Wonderful! This sure is an impressive mill.")
                    chatNpc(
                        happy,
                        "Thank you! I try to keep it clean. Feel free to use the mill whenever " +
                            "you please.",
                    )
                    chatNpc(happy, "Was there anything else you wanted?")
                }
                else -> return bye()
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Preswylwyr_Dall
    private suspend fun Dialogue.preswylwyrDall() {
        greet()
        chatNpc(happy, "Hello! What can I do for you today?")
        while (true) {
            when (choice3("Who are you?", 1, "What do you do here?", 2, "Bye!", 3)) {
                1 -> whoAreYou("Preswylwyr", "I'm Preswylwyr.")
                2 -> {
                    chatPlayer(quiz, "What do you do here?")
                    chatNpc(
                        happy,
                        "I'm Gwerin's partner, he runs the mill while it's my job to use up all " +
                            "the flour he produces.",
                    )
                    chatPlayer(happy, "You bake? So that's what the wonderful smell is.")
                    chatNpc(
                        laugh,
                        "Ha ha! Would you care for some bread? There should be some on the table.",
                    )
                    chatPlayer(happy, "Really? That's very nice of you, thanks!")
                    chatNpc(happy, "No problem! Was there anything else you wanted?")
                }
                else -> return bye()
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Marwolaeth_Dawnsio
    private suspend fun Dialogue.marwolaethDawnsio() {
        greet()
        chatNpc(happy, "Hello! What can I do for you today?")
        while (true) {
            val option =
                choice4(
                    "Who are you?",
                    1,
                    "What do you do here?",
                    2,
                    "Can you tell me about this place?",
                    3,
                    "Bye!",
                    4,
                )
            when (option) {
                1 -> whoAreYou("Marwolaeth", "I'm Marwolaeth, bringer of prophecy.")
                2 -> prophecy()
                3 -> gwenithTown()
                else -> return bye()
            }
        }
    }

    // TODO(content): [system:god_items] the Saradomin and Zamorak readings when wearing an item
    // aligned to either god
    private suspend fun Dialogue.prophecy() {
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "I'm the local wisdom, my dreams are those of prophecy. Would you care for a reading?",
        )
        when (choice2("Yes, tell me of my future...", 1, "Hmm, no thanks...", 2)) {
            1 -> {
                chatPlayer(quiz, "Yes, tell me of my future...")
                chatNpc(
                    neutral,
                    "I see peace and prosperity - The air is filled with wonderous bird song and " +
                        "the scent of earth and river.",
                )
                chatPlayer(confused, "What's that supposed to mean?")
                chatNpc(
                    neutral,
                    "It isn't my place to derive meaning. Allow my foretelling to guide your " +
                        "future but do not rely upon it happening all by itself. We are the " +
                        "arbiters of our own destiny.",
                )
                chatPlayer(
                    confused,
                    "That's really quite vague, do you just say this to everyone?",
                )
                chatNpc(neutral, "It's your choice to believe, not mine.")
                chatPlayer(neutral, "Oh, alright... Thanks, I suppose.")
                chatNpc(happy, "You're most welcome, ${player.displayName}.")
            }
            2 -> {
                chatPlayer(neutral, "Hmm, no thanks...")
                chatNpc(quiz, "Are you sure?")
                chatPlayer(
                    worried,
                    "I'd prefer to stay ignorant... Who knows what that knowledge could do to a " +
                        "person...",
                )
                chatNpc(neutral, "A blissful existence may be the correct path.")
            }
        }
        chatNpc(happy, "Was there anything else you wanted?")
    }

    private suspend fun Dialogue.gwenithTown() {
        chatPlayer(quiz, "Can you tell me about this place?")
        chatNpc(
            happy,
            "I can! This is the quaint town of Gwenith. All those who have been granted access " +
                "to the great crystal city of Prifddinas are welcome here.",
        )
        chatPlayer(worried, "What's with that creepy mountain?")
        chatNpc(
            neutral,
            "Ah... It is expected for an outsider like yourself to notice the mountain wonder " +
                "how we exist in its shadow.",
        )
        chatNpc(
            neutral,
            "We leave the mountain alone and it lets us live peacefully, out of sight of any " +
                "passing boats.",
        )
        chatPlayer(shocked, "It? The mountain is conscious?")
        chatNpc(
            neutral,
            "Of course! At the very top of the mountain sits a ruin. Whatever calamity happened " +
                "there; their ghosts remain.",
        )
        chatPlayer(shocked, "G-g-ghosts?!")
        chatNpc(
            neutral,
            "Yes. Explore at your own peril, ${player.displayName}. There are those who have " +
                "looked for answers from these mynydd spirits. They never return.",
        )
        chatPlayer(
            neutral,
            "That doesn't necessarily mean they died... They may have found the meaning that " +
                "they desired and have gone elsewhere.",
        )
        chatNpc(
            neutral,
            "Elsewhere? Indeed... As I said, ${player.displayName}, explore at your own peril. " +
                "Was there anything else you wanted?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ed (Mynydd)
    private suspend fun Dialogue.ed() {
        chatPlayer(happy, "Hello.")
        if ("obj.ornate_helm" in player.worn) {
            chatNpc(shocked, "That helmet! Where did you get it?")
            chatPlayer(happy, "I found it.")
            chatNpc(angry, "Agh, not again!")
            return
        }
        chatNpc(happy, "Hello again friend.")
        while (true) {
            val option =
                choice4(
                    "What are you doing all the way over here?",
                    1,
                    "That's a nice set of armour.",
                    2,
                    "Hey your hair is on fire!",
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What are you doing all the way over here?")
                    chatNpc(neutral, "I'm looking for a helmet. I know it's buried somewhere.")
                    chatNpc(
                        neutral,
                        "I gave up on the last one. Who cares about that one, anyway?",
                    )
                    chatPlayer(neutral, "I guess...")
                }
                2 -> {
                    chatPlayer(happy, "That's a nice set of armour.")
                    chatNpc(happy, "So much nicer than the garb I had before, right?")
                    chatNpc(sad, "I just wish I had the helmet to go with it.")
                }
                3 -> {
                    chatPlayer(shocked, "Hey your hair is on fire!")
                    chatNpc(angry, "Get out of it!")
                }
                else -> {
                    chatPlayer(happy, "Goodbye.")
                    chatNpc(happy, "See you later.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ghost_(Mynydd)
    private suspend fun Dialogue.ghost() {
        if (GHOSTSPEAK.none { it in player.worn }) {
            chatNpc(neutral, "Woooo wooo wooooo woooo")
            mesbox("You cannot understand the ghost.")
            return
        }
        chatNpc(worried, "Don't send me back to that library!")
        chatPlayer(confused, "What?")
        chatNpc(worried, "It took me too long to get out of there... Just don't send me back!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lenny
    private suspend fun Dialogue.lenny() {
        mesbox("As you approach Lenny, he takes something out of his pocket.")
        chatPlayer(quiz, "What'd you take out of that pocket?")
        chatNpc(shifty, "Ain't a thing in my pocket...")
        chatPlayer(neutral, "I know there ain't. You got it in your hand...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tide
    private suspend fun Dialogue.tide() {
        chatPlayer(quiz, "Hi. What are you doing out here?")
        chatNpc(happy, "Oh, just looking out at the tide...")
        chatPlayer(confused, "What tide?")
        chatNpc(happy, "Is that a boat I see in the distance?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Slippers
    private suspend fun Dialogue.musician() {
        mesbox("He's too busy playing some succulent beats to stop and talk.")
    }

    private companion object {
        val GHOSTSPEAK =
            listOf(
                "obj.amulet_of_ghostspeak",
                "obj.amulet_of_ghostspeak_enchanted",
                "obj.morytania_legs_medium",
                "obj.morytania_legs_hard",
                "obj.morytania_legs_elite",
            )
    }
}
