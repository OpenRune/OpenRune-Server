package org.rsmod.content.areas.misc.islands.fossilisland.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class LithkrenGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.jardric_lithkren") { startDialogue(it.npc) { jardric() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jardric (after Dragon Slayer II, Lithkren)
    // TODO(content): [system:lithkren_boat] the boat trip to Fossil Island after "Yes please." and
    //  the Travel op; the wiki gives no landing tile
    // TODO(content): [system:treasure_trails] elite challenge scroll
    private suspend fun Dialogue.jardric() {
        chatNpc(happy, "Hello. Wanting to go back to Fossil Island?")
        val option =
            choice3("Yes please.", 1, "How are things with you?", 2, "No thank you.", 3)
        when (option) {
            1 -> chatPlayer(happy, "Yes please.")
            2 -> {
                chatPlayer(quiz, "How are things with you?")
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
            else -> chatPlayer(neutral, "No thank you.")
        }
    }
}
