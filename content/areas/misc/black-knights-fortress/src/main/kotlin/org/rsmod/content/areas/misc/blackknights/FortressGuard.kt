package org.rsmod.content.areas.misc.blackknights

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The guard on the Black Knights' Fortress front gate, and the gate itself. */
class FortressGuard : PluginScript() {
    override fun ScriptContext.startup() {
        for (guard in GUARDS) {
            onOpNpc1(guard) { startDialogue(it.npc) { turnedAway() } }
        }
        onOpLoc1("loc.inaclefayeunopenabledoorr") { mes("You can't open this door.") }
    }

    private suspend fun Dialogue.turnedAway() {
        chatNpc(angry, "Get lost. This is private property.")
        chatPlayer(angry, "Yes, but I work here!")
        chatNpc(
            angry,
            "I might be new here but I can tell that's not true! You're not even wearing proper " +
                "uniform!",
        )
        if (choice2("Oh pleeeaaase let me in!", true, "So what is this uniform?", false)) {
            chatPlayer(sad, "Oh pleeeaaase let me in!")
            chatNpc(angry, "Go away. You're getting annoying.")
            return
        }
        chatPlayer(quiz, "So what is this uniform?")
        chatNpc(
            neutral,
            "Well you can see me wearing it. It's an iron chainbody and a medium bronze helm.",
        )
        chatPlayer(neutral, "Hmmm... I wonder if I can make that or get some in the local towns....")
        chatNpc(quiz, "What was that you muttered?")
        chatPlayer(happy, "Oh, nothing important!")
    }

    private companion object {
        private val GUARDS = listOf("npc.fortressguard_01", "npc.fortressguard_02")
    }
}
