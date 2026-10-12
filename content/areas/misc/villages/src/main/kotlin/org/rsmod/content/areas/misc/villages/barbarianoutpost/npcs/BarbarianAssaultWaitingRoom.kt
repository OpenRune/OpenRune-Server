package org.rsmod.content.areas.misc.villages.barbarianoutpost.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class BarbarianAssaultWaitingRoom : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.barbassault_barbarian_quickstart") { startDialogue(it.npc) { sambur() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Sergeant_Sambur
    private suspend fun Dialogue.sambur() {
        chatPlayer(quiz, "Hi there; what is this room?")
        chatNpc(
            happy,
            "Glad you asked. The first thing you will notice on trying to enter the room is that " +
                "you will be asked to choose either the Attacker, Defender, Collector, Healer or " +
                "'any' role.",
        )
        chatPlayer(neutral, "Okay. I'm following so far.")
        chatNpc(
            neutral,
            "Well, the idea with this room is that we will set up a game for you, choosing some " +
                "other people from within the room. If there are enough players around we will " +
                "start a game for you.",
        )
        chatPlayer(quiz, "I see. So I just have to wait around for a game?")
        chatNpc(
            neutral,
            "If you want, or you can look around the other rooms and see if there is anyone " +
                "waiting around.",
        )
    }
}
