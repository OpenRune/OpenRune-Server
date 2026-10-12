package org.rsmod.content.areas.misc.zanaris.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FairyRingPlanes : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.fairytale2_cosmic_entity") { startDialogue(it.npc) { cosmicBeing() } }
        onOpNpc1("npc.fairy2_centaur_female") {
            startDialogue(it.npc) {
                chatNpc(laugh, "What a funny creature you are! Why, you only have 2 legs!")
            }
        }
        onOpNpc1("npc.fairy2_centaur_male") {
            startDialogue(it.npc) { chatNpc(happy, "Hello human, welcome to our valley.") }
        }
        onOpNpc1("npc.fairy2_dryad") { startDialogue(it.npc) { woodDryad() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cosmic_Being
    // TODO(content): [quest:fairytale_ii_cure_a_queen] "I'm looking for a Star Flower." during the
    // quest
    private suspend fun Dialogue.cosmicBeing() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            happy,
            "Salutations traveller! It is unusual for one of the creatures called 'humans' to " +
                "visit this plane. How can I assist you?",
        )
        when (choice2("I'm just looking around, thanks.", 1, "Who are you?", 2)) {
            1 -> {
                chatPlayer(neutral, "I'm just looking around, thanks.")
                chatNpc(happy, "I hope the beauty of my world brings you great joy.")
            }
            2 -> {
                chatPlayer(quiz, "Who are you?")
                chatNpc(
                    neutral,
                    "My name, in your language, would be Bubrezankaldargarez'jk, but you can " +
                        "just call me Bub. This small plane is my home, here I study and ponder " +
                        "the mysteries of existence.",
                )
                chatPlayer(quiz, "Mysteries of existence, eh? Like what?")
            }
        }
        chatNpc(quiz, MYSTERIES[access.random.of(MYSTERIES.size)])
    }

    // https://oldschool.runescape.wiki/w/Transcript:Wood_Dryad
    private suspend fun Dialogue.woodDryad() {
        chatPlayer(quiz, "Hi, why do you have twigs growing out of you?")
        chatNpc(laugh, "Heehee, what a strange question, that's because I'm a Wood Dryad.")
    }

    private companion object {
        val MYSTERIES =
            listOf(
                "Where do flies go at night?",
                "Have you ever flicked through a dictionary and afterwards felt really depressed " +
                    "by how much you don't know?",
                "Do other birds speak pigeon pidgin?",
                "Political philosophy is an oxymoron.",
                "Why are yawns contagious?",
                "Do sick days include when you're sick of work?",
                "To be is to be the value of a variable.",
                "How do you know when you've run out of invisible ink?",
                "Where does a snake's tail begin?",
                "How long do fish wait to swim after they eat?",
                "If you're halfway from here to there, which is which?",
            )
    }
}
