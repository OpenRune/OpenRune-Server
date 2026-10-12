package org.rsmod.content.areas.city.burthorpe.npcs.warriorsguild

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Harrallak_Menarous (before While Guthix Sleeps)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:while_guthix_sleeps] "How are you, Harrallak?" after-quest dialogue
class Harrallak : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.warguild_harrallak_npc") { startDialogue(it.npc) { harrallak() } }
    }

    private suspend fun Dialogue.harrallak() {
        chatNpc(happy, "Welcome to my humble guild ${player.displayName}.")
        while (true) {
            val option =
                choice3(
                    "Quite a place you've got here.",
                    1,
                    "You any good with a sword?",
                    2,
                    "Bye!",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Quite a place you've got here.")
                    chatNpc(happy, "Indeed we do. Would you like to know more about it?")
                    guildTopics()
                }
                2 -> sword()
                3 -> {
                    chatPlayer(happy, "Bye!")
                    val farewell = if (isMale()) "brave warrior" else "fair lady"
                    chatNpc(happy, "Farewell $farewell, I do hope you enjoy my guild.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.guildTopics(): Nothing {
        while (true) {
            val topic =
                choice5(
                    "Tell me about the strength training area.",
                    1,
                    "Tell me about the attack training area.",
                    2,
                    "Tell me about the defence training area.",
                    3,
                    "Tell me about the combat training area.",
                    4,
                    "Tell me about tokens.",
                    5,
                )
            when (topic) {
                1 -> strengthArea()
                2 -> attackArea()
                3 -> defenceArea()
                4 -> combatArea()
                5 -> tokens()
            }
        }
    }

    private suspend fun Dialogue.strengthArea() {
        chatPlayer(quiz, "Tell me about the strength training area.")
        chatNpc(
            neutral,
            "Ahh the mighty warrior Sloane guards the strength training area. This intriguing " +
                "little area consists of two shotput lanes for different weights of shot. It's " +
                "fairly simple, the Referee or Sloane can explain more. You",
        )
        chatNpc(neutral, "may find yourself panting for breath though.")
        chatPlayer(quiz, "Oh? Why?")
        val title = if (isMale()) "boy" else "lady"
        chatNpc(
            neutral,
            "Well you see my dear $title, the games there really do sap your energy quite a bit. " +
                "You can find it just up the stairs behind the bank.",
        )
    }

    private suspend fun Dialogue.attackArea() {
        chatPlayer(quiz, "Tell me about the attack training area.")
        chatNpc(neutral, "Ahhh, dummies.")
        chatPlayer(angry, "I'm no dummy, I just want to know what is there!")
        val title = if (isMale()) "man" else "lady"
        chatNpc(
            shocked,
            "Oh no my dear $title, I did not mean you at all! The training area has mechanical " +
                "dummies which pop up out of holes in the floor. The noble dwarf Gamfred " +
                "invented the mechanism and Ajjat can explain more about what",
        )
        chatNpc(neutral, "to do there.")
        chatPlayer(happy, "Oh, ok, I'll have to try it out!")
        chatNpc(neutral, "You can find it just down the corridor and on your right.")
    }

    private suspend fun Dialogue.defenceArea() {
        chatPlayer(quiz, "Tell me about the defence training area.")
        chatNpc(
            neutral,
            "To polish your defensive skills to the very highest level we've employed a most " +
                "inventive dwarf and a catapult.",
        )
        chatPlayer(shocked, "You're going to throw dwarves at me?")
        chatNpc(laugh, "Oh my no! I think Gamfred would object to that most strongly.")
        chatNpc(
            neutral,
            "He's an inventor you see and has built a marvellous contraption that can throw all " +
                "sorts of things at you including magic missiles...",
        )
        chatPlayer(worried, "Mmmm?")
        chatNpc(neutral, "....spiked iron balls.....")
        chatPlayer(worried, "Err....")
        chatNpc(neutral, "....spinning slashing blades....")
        chatPlayer(worried, "Ummm...")
        chatNpc(neutral, "... and even anvils.")
        chatPlayer(shocked, "ANVILS!?")
        chatNpc(
            neutral,
            "No need to be afraid, it's all under very controlled conditions! You can find it " +
                "just up the stairs behind the bank.",
        )
    }

    private suspend fun Dialogue.combatArea() {
        chatPlayer(quiz, "Tell me about the combat training area.")
        chatNpc(
            neutral,
            "Ahh yes, our resident magician from foreign lands created a most amazing gadget " +
                "which can turn your own armour against you! It's really quite intriguing.",
        )
        chatPlayer(worried, "That sounds dangerous. What if I'm wearing it at the time?")
        chatNpc(
            neutral,
            "So far that's not happened. You need to speak to Shanomi about the specifics of the " +
                "process, but as I understand it, putting a suit of armour in one of these " +
                "devices will make it come to life some how. The better",
        )
        chatNpc(neutral, "the armour, the harder it is to 'kill'.")
        chatPlayer(
            worried,
            "Fighting my own armour, that sounds... weird. I could be killed by it...",
        )
        chatNpc(
            neutral,
            "Indeed we have had a few fatalities from warriors over stretching themselves and " +
                "not knowing their limits. Start small and work up is my motto! That and go see " +
                "Lidio for some food if you need it.",
        )
        chatPlayer(neutral, "Ok, thanks for the warning.")
    }

    private suspend fun Dialogue.tokens() {
        chatPlayer(quiz, "Tell me about tokens.")
        chatNpc(
            neutral,
            "Ahh, yes, our token system is designed to allow you an appropriate amount of time " +
                "with my discovery in the very top floor of the guild. I won't spoil the " +
                "surprise as to what that is. Go up and see for yourself. Now, the",
        )
        chatNpc(
            neutral,
            "amount of tokens you collect from the various activities around the guild will " +
                "dictate how long Kamfreena will allow you in the enclosure on the very top " +
                "floor. More tokens equals more time.",
        )
        chatPlayer(quiz, "So what's up there?")
        chatNpc(laugh, "If I told you it would spoil the surprise!")
        chatPlayer(quiz, "Ok ok... so how do I earn and claim these tokens?")
        chatNpc(
            neutral,
            "You can earn them simply by using the training exercises around the guild, the " +
                "staff will then enter this into a ledger as you play. You can claim them by " +
                "simply asking any of the staff at the training areas, or",
        )
        chatNpc(neutral, "myself.")
        chatPlayer(happy, "Sounds easy enough.")
    }

    private suspend fun Dialogue.sword() {
        chatPlayer(quiz, "You any good with a sword?")
        chatNpc(shocked, "Am I any good with....a sword... Have you any clue who I am?")
        chatPlayer(neutral, "Not really... no.")
        chatNpc(neutral, "Why I could best any person alive in a rapier duel!")
        chatPlayer(angry, "Try me then!")
        val title = if (isMale()) "sir" else "lady"
        chatNpc(
            neutral,
            "My dear $title, I couldn't possibly duel you, I might hurt you and then what would " +
                "happen to my reputation! Besides, I have this wonderful guild to run. Why don't " +
                "you take a look at the various activities we",
        )
        chatNpc(
            neutral,
            "have. You might even collect enough tokens to be allowed in to kill the strange " +
                "beasts from the east!",
        )
    }
}
