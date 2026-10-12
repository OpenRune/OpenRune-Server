package org.rsmod.content.areas.zeah.hosidius.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Vannah
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server inv for Vannah's Farming Stall (Trade op)
class VannahsFarmingStall : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_farmingstore") { startDialogue(it.npc) { vannah() } }
    }

    private suspend fun Dialogue.vannah() {
        chatNpc(happy, "Need some farming supplies?")
        while (true) {
            val option =
                choice3("What is this place?", 1, "What have you got?", 2, "No, I'm fine.", 3)
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What is this place?")
                    chatNpc(
                        happy,
                        "It's a farming supply stall. Hosidius is farming country, so it's a big " +
                            "business. Do you need anything now?",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "What have you got?")
                    // TODO(content): [system:shops] opens Vannah's Farming Stall
                    return
                }
                else -> {
                    chatPlayer(neutral, "No, I'm fine.")
                    return
                }
            }
        }
    }
}
