package org.rsmod.content.areas.city.canifis.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class CanifisTownsfolk : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.poh_taxidermist") { startDialogue(it.npc) { taxidermist() } }
        onOpNpc1("npc.mort_myre_gate_guard") { startDialogue(it.npc) { ulizius() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Taxidermist
    // TODO(content): [system:poh] using heads and big fish on her to have them stuffed
    private suspend fun Dialogue.taxidermist() {
        chatNpc(happy, "Oh, hello. Have you got something you want preserving?")
        when (choice3("Yes please", 1, "Not right now", 2, "What?", 3)) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                chatNpc(happy, "Give it to me to look at then.")
            }
            2 -> {
                chatPlayer(neutral, "Not right now.")
                chatNpc(happy, "Well, you go kill things so I can stuff them, eh?")
            }
            3 -> {
                chatPlayer(confused, "What?")
                chatNpc(
                    neutral,
                    "If you bring me a monster head or a very big fish, I can preserve it for " +
                        "you so you can mount it in your house.",
                )
                chatNpc(
                    happy,
                    "I hear there are all sorts of exotic creatures in the Slayer Tower -- I'd " +
                        "like a chance to stuff one of them!",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ulizius (before Nature Spirit)
    // TODO(content): [quest:nature_spirit] in-progress and after-quest answers and the gate
    // TODO(content): [system:treasure_trails] medium clue step reward
    private suspend fun Dialogue.ulizius() {
        chatPlayer(happy, "Hello there.")
        chatNpc(
            shocked,
            "What... Oh, don't creep up on me like that... I thought you were a Ghast!",
        )
        chatPlayer(quiz, "Can I go through the gate please?")
        chatNpc(
            angry,
            "Absolutely not! I've been given strict instructions not to let anyone through. It's " +
                "just too dangerous. No one gets in without Drezels say so!",
        )
        chatPlayer(quiz, "Where is Drezel?")
        chatNpc(
            neutral,
            "Oh, he's in the temple, just go back over the bridge, down the ladder and along the " +
                "hallway, you can't miss him.",
        )
    }
}
