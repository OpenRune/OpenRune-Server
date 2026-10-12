package org.rsmod.content.generic.npcs.gnome

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [verify:in_game] the wiki's "Normal" child (gnomechildgreen) is taken to be the
// transcript's "Green with hat" child, and gnomechild its "Green" child
// TODO(content): [verify:in_game] the worm box shows the unobtainable obj.worm; no item is given
// https://oldschool.runescape.wiki/w/Transcript:Gnome_child
class GnomeChildren : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gnomechildgreen") { startDialogue(it.npc) { greenWithHat() } }
        onOpNpc1("npc.gnomechildblue") { startDialogue(it.npc) { blue() } }
        onOpNpc1("npc.gnomechild") { startDialogue(it.npc) { green() } }
    }

    private suspend fun Dialogue.greenWithHat() {
        chatPlayer(happy, "Hello.")
        when (access.random.of(CONVERSATIONS)) {
            0 -> {
                chatNpc(happy, "I have a riddle for you.")
                chatPlayer(neutral, "OK.")
                chatNpc(
                    happy,
                    "A tree which is planted on Monday and doubles in size each day is fully " +
                        "grown on the following Sunday. On what day is it half grown?",
                )
                chatPlayer(confused, "Erm... I'm not sure.")
                chatNpc(laugh, "Saturday. You big folk really aren't the quickest!")
            }
            1 -> {
                chatNpc(neutral, "To be or not to be.")
                chatPlayer(quiz, "Hey I know that. Where's it from?")
                chatNpc(happy, "Existentialism for insects.")
            }
            2 -> {
                chatNpc(
                    happy,
                    "My mum says: A friendly look, a kindly smile one good act, and life's " +
                        "worthwhile!",
                )
                chatPlayer(happy, "Sweet.")
            }
            3 -> {
                chatNpc(happy, "I wrote a few children's books.")
                chatPlayer(quiz, "Really.")
                chatNpc(laugh, "Yep... but not on purpose!")
            }
            4 -> chatNpc(neutral, "The human mind is a tremendous thing.")
            5 -> {
                chatNpc(laugh, "Bla bla bla!")
                chatPlayer(confused, "What?")
                chatNpc(laugh, "Bla bla bla!")
                mesbox("Rude little gnome!")
            }
            6 -> {
                chatNpc(
                    neutral,
                    "Real generosity is doing something nice for someone who will never find it " +
                        "out.",
                )
                chatPlayer(happy, "Thanks for the quote.")
            }
            7 -> {
                chatNpc(happy, "Nice weather we're having today.")
                chatNpc(happy, "But then it doesn't tend to rain much around here.")
            }
            8 -> {
                chatNpc(laugh, "Hardy, ha ha! Hee, hee hee!")
                chatPlayer(quiz, "Are you OK?")
                chatNpc(happy, "I'm a little tree gnome. That is me!")
                chatPlayer(neutral, "I've heard better.")
            }
            9 -> {
                chatNpc(happy, "I have a riddle for you.")
                chatPlayer(neutral, "OK.")
                chatNpc(
                    happy,
                    "I am the beginning of eternity and the end of time and space. I am the " +
                        "beginning of every end and the end of every place. What am I?",
                )
                chatPlayer(confused, "Err...?")
                chatPlayer(confused, "Erm...not sure...annoying.")
                chatNpc(laugh, "I'm 'e'! Hee hee! Do you get it?")
            }
            10 -> {
                chatNpc(happy, "I worship Guthix, the god of balance.")
                chatNpc(happy, "He really does have exceptional co-ordination.")
            }
            11 -> {
                chatPlayer(quiz, "Hello, are you alright?")
                chatNpc(sad, "Not really, I just saw a man with a wooden leg.")
                chatPlayer(confused, "So?!")
                chatNpc(laugh, "He had a real foot!")
            }
            else -> {
                chatPlayer(quiz, "What are you doing?")
                chatNpc(happy, "I'm making wine out of raisins.")
                chatPlayer(quiz, "Why?")
                chatNpc(happy, "So I don't have to wait for it to age.")
                chatNpc(laugh, "Hee hee!")
            }
        }
    }

    private suspend fun Dialogue.blue() {
        chatPlayer(happy, "Hi.")
        when (access.random.of(CONVERSATIONS)) {
            0 -> {
                chatNpc(happy, "She loves me!")
                chatPlayer(neutral, "Really.")
                chatNpc(happy, "She does I tell you. She really loves me!")
            }
            1 -> {
                chatNpc(happy, "Hello.")
                chatPlayer(quiz, "Are you alright?")
                chatNpc(sad, "I just want something to happen!")
                chatPlayer(quiz, "Like what?")
                chatNpc(sad, "Something, anything, I don't know what!")
            }
            2 -> {
                chatNpc(happy, "A little inaccuracy sometimes saves tons of explanation!")
                chatPlayer(neutral, "True.")
            }
            3 -> {
                chatNpc(quiz, "Hello. Why are you so tall?")
                chatPlayer(confused, "Um... I've always been this height.")
                chatNpc(happy, "Maybe you should eat more worms.")
            }
            4 -> {
                chatNpc(happy, "Low.")
                chatPlayer(confused, "What?")
                chatNpc(happy, "When?")
                chatPlayer(angry, "Cheeky!")
                chatNpc(laugh, "Hee hee!")
            }
            5 -> {
                mesbox("The gnome is praying.")
                chatNpc(
                    neutral,
                    "Guthix's angels fly so high as to be beyond our sight but they are always " +
                        "looking down upon us.",
                )
            }
            6 -> {
                chatNpc(happy, "Hello, would you like a worm?")
                chatPlayer(confused, "Erm... OK.")
                objbox("obj.worm", "The gnome gives you a worm")
                chatPlayer(happy, "Thanks.")
                chatNpc(
                    happy,
                    "In the gnome village those who are needy receive what they need, and those " +
                        "who are able give what they can.",
                )
            }
            7 -> {
                chatNpc(neutral, "Dimensionality is a function of consciousness.")
                chatPlayer(confused, "What?")
                chatNpc(neutral, "That's all you need to know.")
            }
            8 -> {
                chatNpc(neutral, "I've been thinking.")
                chatPlayer(neutral, "OK.")
                chatNpc(neutral, "Surely, if you're scared to die...")
                chatNpc(neutral, "...you're not living, right?")
                chatPlayer(neutral, "Maybe!")
            }
            9 -> {
                chatPlayer(quiz, "How are you?")
                chatNpc(
                    worried,
                    "A warning traveller! The new world will rise from the underground!",
                )
                chatPlayer(quiz, "What do you mean, underground?")
                chatNpc(worried, "Just a warning!")
            }
            10 -> {
                chatNpc(
                    neutral,
                    "Some advice traveller. We can walk, run, row or fly but never lose sight " +
                        "of the reason for the journey or miss the chance to see a rainbow on " +
                        "the way.",
                )
                chatPlayer(happy, "I like that.")
            }
            11 -> {
                chatPlayer(happy, "You look happy.")
                chatNpc(happy, "I'm always at peace with myself.")
                chatPlayer(quiz, "How do you manage that?")
                chatNpc(happy, "I know, therefore I am.")
            }
            else -> {
                mesbox("The gnome appears to be singing.")
                chatNpc(happy, "Oh baby, oh my sweet.")
                chatPlayer(quiz, "Are you talking to me?")
                chatNpc(happy, "No, I'm just singing.")
                chatNpc(happy, "I'm gonna sweep you off your feet.")
            }
        }
    }

    private suspend fun Dialogue.green() {
        chatPlayer(happy, "Hi there.")
        when (access.random.of(CONVERSATIONS)) {
            0 -> {
                val address = if (access.isBodyTypeA()) "man" else "woman"
                chatNpc(happy, "Hey big $address, watch where you're standing.")
                chatNpc(happy, "I wouldn't want to make a mess of your shoes.")
            }
            1 -> {
                chatNpc(happy, "Have you seen our big tree?")
                chatPlayer(happy, "Yes I have!")
                chatNpc(happy, "It's not small!")
            }
            2 -> {
                chatNpc(happy, "Have you seen our big tree?")
                chatPlayer(happy, "Yes I have!")
                chatNpc(happy, "It's very grand.")
                chatNpc(laugh, "Hee hee!")
            }
            3 -> {
                chatNpc(neutral, "You are the contained centre of an O.")
                chatPlayer(confused, "Pardon?")
                chatNpc(neutral, "While me, I'm much more. I am the pyramidic containment of an A.")
                chatPlayer(confused, "You're strange.")
            }
            4 -> {
                chatNpc(happy, "I'm trying to make a comeback!")
                chatPlayer(quiz, "Really?")
                chatNpc(sad, "It's hard when you haven't been anywhere.")
            }
            5 -> chatNpc(shocked, "Aaaaarrggghh... humans! Run for your lives!")
            6 -> {
                chatNpc(happy, "Hello brave adventurer. A warrior like you should go a long way.")
                chatPlayer(happy, "Thanks.")
                chatNpc(laugh, "The further, the better! Hee hee!")
            }
            7 -> {
                chatNpc(neutral, "Es ataris eto meriz ip prit es gutus!")
                chatPlayer(confused, "Pardon?")
                chatNpc(neutral, "Es ataris eto meriz ip prit es gutus!")
            }
            8 -> {
                chatNpc(sad, "Oh hello.")
                chatPlayer(quiz, "How are you?")
                chatNpc(
                    sad,
                    "Not great, it's just... well... people just don't seem to take me seriously!",
                )
            }
            9 -> {
                chatNpc(happy, "How are you?")
                chatPlayer(happy, "I'm fine thanks.")
            }
            10 -> {
                chatNpc(happy, "Why don't you make like a tree...")
                chatNpc(laugh, "...and leave!")
                chatPlayer(neutral, "Funny!")
                chatNpc(laugh, "Hee hee!")
            }
            11 -> {
                chatNpc(happy, "Top of the morning to you.")
                chatPlayer(happy, "Thanks.")
                chatNpc(laugh, "And bottom of the afternoon.")
            }
            else -> {
                chatNpc(neutral, "The only thing that is constant...")
                chatNpc(neutral, "...is change!")
            }
        }
    }

    private companion object {
        const val CONVERSATIONS = 13
    }
}
