package org.rsmod.content.areas.zeah.hosidius.npcs.shops

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Logava
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:shops] no server inv for Logava Gricoller's Cooking Supplies (Trade op)
// TODO(content): [skill:thieving] "Hey! You're that thief!" shortly after stealing from a stall
class LogavaCookingSupplies : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.hosidius_foodstore") { startDialogue(it.npc) { logava() } }
    }

    private suspend fun Dialogue.logava() {
        chatNpc(happy, "Hello, can I help you?")
        while (true) {
            when (choice3("Let's trade.", 1, "Where am I?", 2, "No, I'm okay.", 3)) {
                1 -> {
                    chatPlayer(happy, "Let's trade.")
                    // TODO(content): [system:shops] opens Logava Gricoller's Cooking Supplies
                    return
                }
                2 -> {
                    chatPlayer(quiz, "Where am I?")
                    chatNpc(
                        happy,
                        "This is the marketplace of Hosidius. To the north west there's my " +
                            "sister's pub, and in the south is my grandfather's fruit farm. Do " +
                            "you need to buy anything while you're here?",
                    )
                }
                else -> {
                    chatPlayer(neutral, "No, I'm okay.")
                    return
                }
            }
        }
    }
}
