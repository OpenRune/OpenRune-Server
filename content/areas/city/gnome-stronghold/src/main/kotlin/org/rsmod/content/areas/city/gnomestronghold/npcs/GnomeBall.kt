package org.rsmod.content.areas.city.gnomestronghold.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class GnomeBall : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gnomereferee") { startDialogue(it.npc) { referee() } }
        onOpNpc1("npc.gnome_ball_coach") { startDialogue(it.npc) { coach() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome_ball_referee (first talk)
    // TODO(content): [system:gnomeball] "Okay then, I'll have a go." whistles and hands over a
    //  gnome ball to start a game
    // TODO(content): [system:met_flags] the "welcome back" and ball-in-play dialogue
    // TODO(content): [system:treasure_trails] medium challenge scroll
    private suspend fun Dialogue.referee() {
        chatNpc(
            happy,
            "Hi, welcome to Gnomeball. It's pretty simple really: You take the ball from me, " +
                "charge at the gnome defence and try to throw the ball through the net using " +
                "your ranged skill.",
        )
        chatNpc(
            happy,
            "It's a rough game but great fun. It's also great for improving agility. So do you " +
                "fancy a game?",
        )
        if (!choice2("Looks too dangerous for me!", false, "Okay then, I'll have a go.", true)) {
            chatPlayer(worried, "Looks too dangerous for me!")
            chatNpc(neutral, "You may be right, we've seen humans die on this field.")
            return
        }
        chatPlayer(happy, "Okay then, I'll have a go.")
        chatNpc(
            happy,
            "Great stuff! There are few rules to Gnomeball, so it can get a bit rough.",
        )
        chatNpc(
            happy,
            "You can pass to the green winger gnomes. Then you can make a run and they'll pass " +
                "back. Otherwise, if you're feeling brave, you can just charge and dodge.",
        )
        chatPlayer(neutral, "Sounds easy enough.")
        chatNpc(
            laugh,
            "The main aim is to leave with no broken limbs. I think you should be fine.",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gnome_Coach
    // TODO(content): [system:npc_overhead] random coaching shouts
    // TODO(content): [system:gnome_restaurant] delivery hand-in dialogue
    private suspend fun Dialogue.coach() {
        chatNpc(angry, "Run faster! Faster!")
        chatPlayer(quiz, "Excuse me?")
        chatNpc(neutral, "Oh, sorry, I didn't notice you there.")
        chatPlayer(quiz, "I take it you're a coach?")
        chatNpc(neutral, "That I am. Now get to the point, I'm very busy!")
        while (true) {
            val option =
                choice4(
                    "What's the History of Gnome ball?",
                    1,
                    "Are there any special tactics?",
                    2,
                    "Does the game bring fame?",
                    3,
                    "Yawn",
                    4,
                )
            when (option) {
                1 -> history()
                2 -> tactics()
                3 -> fame()
                else -> {
                    chatPlayer(bored, "Yawn.")
                    chatNpc(angry, "I beg your pardon?")
                    chatPlayer(neutral, "Erm... thanks, catch you later.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.history() {
        chatPlayer(quiz, "What's the history of Gnome ball?")
        chatNpc(neutral, "Be more precise, stranger.")
        val option =
            choice3(
                "How did it all start?",
                1,
                "What is the ball made from?",
                2,
                "Those nets are strange.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "How did it all start?")
                chatNpc(neutral, "Strangely enough, by accident.")
                chatPlayer(quiz, "Accident?")
                chatNpc(angry, "Don't interrupt!")
                chatPlayer(sad, "Sorry.")
                chatNpc(
                    neutral,
                    "A great many years ago, before I was even born, there were two young " +
                        "Gnomes. Brothers to be exact.",
                )
                chatNpc(
                    neutral,
                    "They were always arguing with each other over the most trivial things - " +
                        "who was the faster runner or who was the tallest.",
                )
                chatNpc(neutral, "One day they were up at the swamp throwing stones at the toads.")
                chatPlayer(neutral, "As you do.")
                chatNpc(happy, "Yep. You know nothing tastes better than toasted toad?")
                chatPlayer(neutral, "Oh, I'm sure")
                chatNpc(
                    neutral,
                    "Anyway, they decided to compete to see who could kill the most toads. This " +
                        "gradually became a daily game, to which they invited friends and " +
                        "family to join in.",
                )
                chatNpc(
                    neutral,
                    "It all evolved from there. Creating a playing arena, making up teams and " +
                        "leagues.",
                )
                chatPlayer(neutral, "Interesting.")
                chatNpc(neutral, "Anything else?")
            }
            2 -> {
                chatPlayer(quiz, "What is the ball made from?")
                chatNpc(neutral, "Toads of course")
                chatPlayer(shocked, "Toads?")
                chatNpc(
                    neutral,
                    "What with the swamp nearby, and their robust skin, there is nothing better " +
                        "to stretch over some common sand.",
                )
                chatPlayer(neutral, "I'd never have guessed.")
                chatNpc(laugh, "Well, of course. You humans know nothing!")
                chatNpc(neutral, "Anything else?")
            }
            3 -> {
                chatPlayer(quiz, "Those nets are strange.")
                chatNpc(neutral, "Ah, made from the finest thread in all the land.")
                chatPlayer(quiz, "And that is?")
                chatNpc(neutral, "Gnome hair.")
                chatPlayer(shocked, "You're kidding, right?")
                chatNpc(
                    angry,
                    "I have no time for jokes. It's a serious matter, we go through plenty of " +
                        "nets every day.",
                )
                chatPlayer(quiz, "So that's why you don't see many Gnomes with long hair?")
                chatNpc(neutral, "Exactly.")
                chatPlayer(laugh, "The world makes sense at last.")
                chatNpc(neutral, "Anything else?")
            }
        }
    }

    private suspend fun Dialogue.tactics() {
        chatPlayer(quiz, "Are there any special tactics?")
        chatNpc(neutral, "Well of course, like what?")
        val option =
            choice4(
                "How about in passing?",
                1,
                "How about teamwork?",
                2,
                "What about my personal physique?",
                3,
                "Any information would be appreciated!",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "How about passing?")
                chatNpc(neutral, "There's the good old one-two tactic.")
                chatPlayer(quiz, "And that is?")
                chatNpc(
                    angry,
                    "You don't know anything do you? You pass the ball onto a team mate, " +
                        "giving him all the attention of your opposition, and giving you the " +
                        "chance to run further ahead.",
                )
                chatPlayer(quiz, "Then what?")
                chatNpc(
                    neutral,
                    "Well, he passes back to you and you've made an advantage. It's useful in a " +
                        "variety of situations!",
                )
                chatPlayer(happy, "I'll remember that one.")
                chatNpc(neutral, "Anything else?")
            }
            2 -> {
                chatPlayer(quiz, "How about teamwork?")
                chatNpc(
                    neutral,
                    "Well, don't forget that you can block the opposition whilst a team mate " +
                        "has the ball. Very useful if you want to stop or slow down anyone " +
                        "approaching him.",
                )
                chatPlayer(happy, "Block for team mates. Got it.")
                chatNpc(neutral, "Anything else?")
            }
            3 -> {
                chatPlayer(quiz, "What about my personal physique?")
                chatNpc(laugh, "You'd look better if you were shorter.")
                chatPlayer(angry, "I meant in regards to scoring more goals!")
                chatNpc(
                    neutral,
                    "I see. Your agility is important if you want to avoid being tackled, so " +
                        "that would be a good attribute to improve. Also many Gnomes say that " +
                        "being able to attack well with a ranged weapon makes you better at",
                )
                chatNpc(neutral, "scoring goals.")
                chatPlayer(neutral, "I suppose I should go away and work on those skills.")
                chatNpc(
                    neutral,
                    "Yes, although playing the game itself will improve these skills too. Also " +
                        "you should remember that your distance from the goal affects your " +
                        "chance of scoring, so get close enough and your ranged ability won't " +
                        "matter as",
                )
                chatNpc(neutral, "much.")
            }
            4 -> {
                chatPlayer(quiz, "Any information would be appreciated!")
                chatNpc(neutral, "Hmm, some general facts?")
                chatPlayer(happy, "Please!")
                chatNpc(
                    neutral,
                    "OK. Have you noticed the different colour clothes for your opposing Gnomes?",
                )
                chatPlayer(neutral, "Nope.")
                chatNpc(
                    neutral,
                    "The colours correspond to their skill level. The red ones are the " +
                        "professionals. They've been playing since before you were born! The " +
                        "orange ones are in the middle of their careers, whereas the yellow " +
                        "players know very",
                )
                chatNpc(laugh, "little. Just like you!")
                chatPlayer(sad, "Thanks... Anything else?")
                chatNpc(
                    neutral,
                    "It's always good to get a group of friends to play with you. That way you " +
                        "can score goals more easily, and if you take it in turns to score the " +
                        "goals you'll all reap the rewards.",
                )
            }
        }
    }

    private suspend fun Dialogue.fame() {
        chatPlayer(quiz, "Does the game bring fame?")
        chatNpc(
            shocked,
            "You mean you've never heard of Speedy Sam The Striker or Norda 'Steal Arms' van " +
                "Druman?",
        )
        chatPlayer(neutral, "Afraid not.")
        chatNpc(shocked, "Not even Great Thunder Legs MacKnee?")
        chatPlayer(neutral, "Nope.")
        chatNpc(neutral, "Well, they're all richer than you'll ever be!")
        chatPlayer(neutral, "Good for them.")
        chatNpc(neutral, "Indeed, anything else?")
    }
}
