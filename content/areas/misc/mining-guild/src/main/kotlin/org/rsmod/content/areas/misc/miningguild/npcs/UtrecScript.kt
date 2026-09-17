package org.rsmod.content.areas.misc.miningguild.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class UtrecScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.mguild_amethystminer") { talk(it.npc) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { conversation() }

    private suspend fun Dialogue.conversation() {
        chatNpc(neutral, "All this mining is tiring work.")
        chatPlayer(quiz, "But you're not even mining at the moment.")
        chatNpc(angry, "Hey! I've been mining all day thank you very much! I'm allowed a break ain't I?")
        chatPlayer(neutral, "Yes, I suppose.")
        chatNpc(neutral, "I should think so. Anyway did you need anything?")
        if (choice2("What are these rocks?", true, "No thanks.", false)) {
            aboutAmethyst()
        } else {
            chatPlayer(neutral, "No thanks.")
            chatNpc(neutral, "Well be on your way then. I've got mining to do!")
        }
    }

    private suspend fun Dialogue.aboutAmethyst() {
        chatPlayer(quiz, "What are these rocks?")
        chatNpc(neutral, "Rocks? They're not rocks, they're minerals!")
        chatPlayer(quiz, "Oh okay. What are they though?")
        chatNpc(
            neutral,
            "Amethyst! We use it a lot back in Keldagrim but this is the only place we've managed to " +
                "find it.",
        )
        chatPlayer(quiz, "What can I use it for?")
        chatNpc(
            neutral,
            "We've found it's quite good for ranging ammunition, something you adventuring lot might " +
                "like.",
        )
        chatNpc(neutral, "We use it a lot for Construction back in Keldagrim as well.")
        chatPlayer(happy, "Awesome! Does that mean I can use it in my house?")
        chatNpc(neutral, "No.")
        chatPlayer(quiz, "Why not?")
        chatNpc(
            angry,
            "Because I said so that's why! Even the best human builders wouldn't know how to build " +
                "with this stuff.",
        )
        if (player.statBase("stat.construction") < 50) {
            chatNpc(laugh, "And you're a long way off the best, believe me.")
        }
        chatPlayer(sad, "Awww.")
    }
}
