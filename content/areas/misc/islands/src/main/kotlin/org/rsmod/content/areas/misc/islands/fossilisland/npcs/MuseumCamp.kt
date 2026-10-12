package org.rsmod.content.areas.misc.islands.fossilisland.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MuseumCamp : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fossil_charles") { startDialogue(it.npc) { charles() } }
        onOpNpc1("npc.fossil_david_camp") { startDialogue(it.npc) { david() } }
        onOpNpc1("npc.fossil_john") { startDialogue(it.npc) { john() } }
        onOpNpc1("npc.fossil_john_camp") { startDialogue(it.npc) { sleepingJohn() } }
        onOpNpc1("npc.fossil_twitcher") { startDialogue(it.npc) { mattimeo() } }
        onOpNpc1("npc.fossil_chemist") { startDialogue(it.npc) { irene() } }
        onOpNpc1("npc.jardric_camp") { startDialogue(it.npc) { jardric() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Charles_(Fossil_Island)
    // TODO(content): [verify:in_game] the transcript lists no option that ends the conversation
    private suspend fun Dialogue.charles() {
        chatNpc(happy, "Hello.")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "What is going on here?",
                    2,
                    "What should I do?",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    val title = if (access.isBodyTypeA()) "sir" else "madam"
                    chatNpc(happy, "Charlie Windar, natural historian, at your service $title")
                    chatPlayer(quiz, "What do you do?")
                    chatNpc(
                        happy,
                        "I study the marvelous creatures and plants that are here on this " +
                            "island. There appears to have been a much higher rate of evolution " +
                            "here than on the mainland. I suspect an outside influence from " +
                            "something.",
                    )
                }
                2 -> {
                    chatNpc(neutral, "We're exploring this island.")
                    chatPlayer(quiz, "What have you found?")
                    chatNpc(
                        happy,
                        "Several very interesting species of fungi and insects so far. But I " +
                            "haven't wandered much beyond the camp yet.",
                    )
                }
                3 -> chatNpc(happy, "Take the chance to explore!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:David_(Fossil_Island)
    private suspend fun Dialogue.david() {
        if (vars[COOKING_POT] == 0) {
            chatNpc(sad, "Build us a stove mate, I'm absolutely starvin!")
            chatPlayer(neutral, "You must be really tired from the journey, you did so much.")
            chatNpc(
                neutral,
                "...So my expertise were not required this time, a man's still gotta eat.",
            )
            chatPlayer(neutral, "I guess I'll get building then...")
            chatNpc(happy, "Cheers mate!")
            return
        }
        chatNpc(confused, "About these eggs... Odd colour, don't cha think?")
        chatPlayer(neutral, "The birds here are a little exotic, apparently.")
        chatNpc(neutral, "Hmm, well, good effort, but I'll pass on the grub this time.")
        chatPlayer(quiz, "What if I gave you a box?")
        chatNpc(neutral, "I'm not sure that'll change my mind.")
        chatPlayer(quiz, "If not a box, then what about a fox?")
        chatNpc(bored, "Nope.")
        chatPlayer(quiz, "A mouse, a house? Would these change your mind?")
        chatNpc(angry, "For the love of Saradomin, please leave me be")
        chatPlayer(happy, "I will! I promise! Just try them and you'll see.")
        chatNpc(angry, "Alright! Fine! I concede.")
        chatPlayer(silent, "...")
        chatNpc(silent, "...")
        chatPlayer(quiz, "Thoughts?")
        chatNpc(neutral, "Yeah, I guess they're nice")
    }

    // https://oldschool.runescape.wiki/w/Transcript:John_(Fossil_Island)
    // TODO(content): [verify:in_game] the transcript lists no option that ends the conversation
    private suspend fun Dialogue.john() {
        chatNpc(happy, "Hello there!")
        while (true) {
            val option =
                choice3(
                    "Who are you?",
                    1,
                    "What is going on here?",
                    2,
                    "What should I do?",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Who are you?")
                    chatNpc(
                        happy,
                        "I'm John, I specialise in archaeology and there's some very " +
                            "interesting ruins in this area.",
                    )
                    chatPlayer(quiz, "Is that like fossils and stuff?")
                    chatNpc(
                        neutral,
                        "Oh my word no. Archaeology is the study of sentient races history " +
                            "through study of artefacts and ruins.",
                    )
                    chatPlayer(quiz, "Weren't fossils sentient?")
                    chatNpc(neutral, "Well... we have no proof to suggest such.")
                }
                2 -> {
                    chatPlayer(quiz, "What is going on here?")
                    chatNpc(
                        happy,
                        "This is our base camp for exploration of this wonderful island.",
                    )
                    chatPlayer(quiz, "Anything I can do to help?")
                    chatNpc(
                        happy,
                        "No, no. You've already helped admirably. Our lives are much more " +
                            "comfortable thank you.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What should I do?")
                    chatNpc(happy, "Explore! There's so much here we don't understand.")
                    chatPlayer(quiz, "But where?")
                    johnHint()
                }
            }
        }
    }

    private suspend fun Dialogue.johnHint() {
        when (access.random.of(3)) {
            0 ->
                chatNpc(
                    neutral,
                    "I've heard there's a pretty spectacular structure up near the mountain, " +
                        "but I haven't gone there yet.",
                )
            1 ->
                chatNpc(
                    happy,
                    "The paleontologists are getting very excited as there are fossils " +
                        "everywhere. Apparently even the sea birds use them to make their nests!",
                )
            else ->
                chatNpc(
                    quiz,
                    "Have you heard about the wyverns? We saw some of them flying the first day " +
                        "we were here, but haven't found them since.",
                )
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:John
    private suspend fun Dialogue.sleepingJohn() {
        chatNpc(silent, "ZzzzZzzZZzzZZzz.")
        chatPlayer(neutral, "He's sleeping peacefully. Loudly, but peacefully.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Mattimeo
    private suspend fun Dialogue.mattimeo() {
        chatPlayer(happy, "Hello.")
        chatNpc(confused, "Hmm, yes... Hello!")
        chatPlayer(quiz, "...Are you busy?")
        chatNpc(neutral, "Oh umm, no and yes. I'm forever observing, you see.")
        while (true) {
            val option =
                choice3(
                    "Observing what?",
                    1,
                    "You seem a little, twitchy...",
                    2,
                    "Goodbye.",
                    3,
                )
            when (option) {
                1 -> mattimeoBirds()
                2 -> mattimeoTwitchy()
                3 -> {
                    chatPlayer(neutral, "Goodbye.")
                    chatNpc(
                        confused,
                        "...Oh, you're still here. What was that? Oh yes, bye for now!",
                    )
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mattimeoBirds() {
        chatPlayer(quiz, "Observing what?")
        chatNpc(happy, "Oh, erm, birds! Yes, those feathery little flittery things.")
        chatPlayer(bored, "Yes, I know what a bird is.")
        chatNpc(
            neutral,
            "Do you though? The subtleties between different species of birds is quite " +
                "incredible, you see-",
        )
        chatPlayer(
            quiz,
            "But how could I attract them? Y'know, so I could observe them a little closer.",
        )
        chatNpc(
            happy,
            "Ah yes, well, of course! I've travelled around this island and found the best " +
                "spots for bird watching. I placed a ring of stones at these locations!",
        )
        chatPlayer(shifty, "So those would be the best locations to place my traps...")
        chatNpc(
            angry,
            "Traps?! Preposterous! These fine exotic Birds are to be watched and not harmed in " +
                "any way!",
        )
        chatPlayer(
            worried,
            "Did I say traps? Oh, I meant, umm, feeding them! Yes! I could feed them.",
        )
        chatNpc(
            angry,
            "You should not interfere with the birds, just watch, no feeding them anything! " +
                "Not even seeds!",
        )
        chatPlayer(shifty, "Seeds you say...")
    }

    private suspend fun Dialogue.mattimeoTwitchy() {
        chatPlayer(quiz, "You seem a little, twitchy...")
        chatNpc(
            happy,
            "Yes, well, perhaps a bit! There are new exotic birds everywhere on this island! I " +
                "guess I'm getting a little over-excited.",
        )
        chatPlayer(shifty, "Yeah, there sure are a lot of birds... For now.")
        chatNpc(neutral, "It's my fathers fault entirely, constantly talking about falconry...")
        chatPlayer(quiz, "Your father is a hunter? Does he know anything about hunting birds?")
        chatNpc(
            bored,
            "Umm, well of course, falcons ARE hunting birds, they mostly catch varying types of " +
                "kebbits, it's rather mundane.",
        )
        chatPlayer(shifty, "And mice?")
        chatNpc(confused, "Hmmm...")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Irene
    // TODO(content): [verify:in_game] the transcript lists no option that ends the conversation
    private suspend fun Dialogue.irene() {
        chatPlayer(happy, "Hello.")
        while (true) {
            when (choice2("Who are you?", 1, "What's this pool for?", 2)) {
                1 -> chatNpc(happy, "Hello, my name is Irene. I'm a chemist.")
                2 -> irenePool()
            }
        }
    }

    private suspend fun Dialogue.irenePool() {
        chatNpc(
            neutral,
            "We're not entirely sure. I'm here to study it to find out just that.",
        )
        chatPlayer(quiz, "Have you tried....")
        val option =
            choice3(
                "Drinking it?",
                1,
                "Sampling it?",
                2,
                "Putting random things in it?",
                3,
            )
        when (option) {
            1 -> {
                chatNpc(sad, "Well, someone drank it... that didn't turn out so well.")
                chatPlayer(worried, "What happened? Who was it? Are they ok?")
                chatNpc(
                    sad,
                    "Well, she's still alive at the moment. It was my mum, Marie. She's a better " +
                        "chemist than me, but she likes to do things practically. This time it " +
                        "didn't work out so well for her. Lucky I was on hand to help.",
                )
                chatPlayer(sad, "I hope she recovers soon.")
                chatNpc(sad, "Me too.")
            }
            2 -> {
                chatNpc(
                    neutral,
                    "Ahh yes. Taking samples. We've tried all the methods we can think of, but " +
                        "it seems to, well, change somehow.",
                )
                chatPlayer(quiz, "What changes?")
                chatNpc(
                    neutral,
                    "The substance. It's almost like taking a finger off a human and assuming " +
                        "it will stay alive I guess. The sample sort of... goes inert once we " +
                        "take the sample from the main body of the pool.",
                )
                chatPlayer(neutral, "How awkward.")
            }
            3 -> {
                chatNpc(
                    happy,
                    "Yes, we've had a bit of progress there. We've tried adding in a lot of " +
                        "things with varying results. The accident was the best though.",
                )
                mesbox("Irene looks embarassed.")
                chatPlayer(quiz, "What happened?")
                chatNpc(
                    neutral,
                    "Well, I slipped and dropped a fossilised leg bone from something in the " +
                        "pool. It... well it kind of ate it away.",
                )
                chatPlayer(quiz, "Was there anything left?")
                chatNpc(
                    happy,
                    "Oh yes, it was quite interesting. It's as if it restored some of the " +
                        "biological content of the bone and stripped all the mineralisation of " +
                        "fosilisation away. The bone was, rather floppy though. Like when you " +
                        "remove all the",
                )
                chatNpc(happy, "calcium and phosphate from it.")
                chatPlayer(quiz, "Have you tried putting calcium in the pool?")
                chatNpc(
                    neutral,
                    "Calcium and phosphate are highly reactive - you'd never find just those in " +
                        "any quantity! You could maybe find some within minerals around this " +
                        "island that could work.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jardric
    private suspend fun Dialogue.jardric() {
        if (QuestRequirements.hasCompleted(player, DRAGON_SLAYER_II)) {
            jardricAfterQuest()
            return
        }
        chatNpc(happy, "Hello there.")
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            happy,
            "I'm Jardric. Former boatman and now a researcher for Keldagrim. I'm here with the " +
                "Varrock Museum to explore this island and discover its secrets.",
        )
        chatPlayer(quiz, "From boatman to researcher? That's quite the change.")
        chatNpc(
            happy,
            "Aye that it is. I do miss it sometimes, but I get so many opportunities now that I " +
                "never used to. Just look at us! We may well be the first to step foot on this " +
                "island in centuries.",
        )
        chatNpc(happy, "If that isn't exciting, I don't know what is.")
        chatPlayer(happy, "Very true. See you later.")
        chatNpc(happy, "Goodbye for now.")
    }

    // TODO(content): [system:lithkren_boat] the boat to Lithkren on "Yes please." and the
    // Travel option
    private suspend fun Dialogue.jardricAfterQuest() {
        chatNpc(happy, "Hello. Wanting to go to Lithkren?")
        val option =
            choice3(
                "Yes please.",
                1,
                "How are things with you?",
                2,
                "No thank you.",
                3,
                title = "Select an Option",
            )
        when (option) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> {
                chatNpc(
                    happy,
                    "Not too bad. I've been working with the museum to investigate the " +
                        "dragonkin. After what happened they now consider me an expert.",
                )
                chatNpc(
                    happy,
                    "Don't worry though. I've still got enough time to look after this boat for " +
                        "you.",
                )
            }
            3 -> chatPlayer(neutral, "No thank you.")
        }
    }

    private companion object {
        const val COOKING_POT = "varbit.fossil_range"
        const val DRAGON_SLAYER_II = "quest_dragonslayer2"
    }
}
