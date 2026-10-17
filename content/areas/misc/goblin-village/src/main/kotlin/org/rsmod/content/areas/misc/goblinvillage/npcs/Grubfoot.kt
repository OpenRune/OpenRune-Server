package org.rsmod.content.areas.misc.goblinvillage.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Grubfoot, who never gets to finish a thought without a general shouting him down. */
class Grubfoot : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.catwalk_goblin") { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        val pick = random.of(maxExclusive = COMPLAINTS)
        val general = random.pick(WARTFACE, BENTNOZE)
        startDialogue(npc) {
            when (pick) {
                0 -> lonely()
                1 -> notAllowed()
                2 -> chatNpc(angry, "Grubfoot do this! Grubfoot do that! They always want different things and they never satisfied!")
                3 -> toldToShutUp()
                else -> chatNpc(sad, "Me wish me weren't so small. Other goblins all pick on me and make me do all hard work for generals.")
            }
            shoutedDown(general)
        }
    }

    private suspend fun Dialogue.lonely() {
        chatNpc(sad, "Me lonely.")
        chatPlayer(quiz, "Why?")
        chatNpc(
            sad,
            "Other goblins in village follow either General Wartface or General Bentnoze. Me " +
                "try to follow both but then me get left out of both groups.",
        )
    }

    private suspend fun Dialogue.notAllowed() {
        chatNpc(sad, "Don't talk to me!")
        chatPlayer(quiz, "Why not?")
        chatNpc(sad, "Me not allowed to talk. Generals will tell me to shut up whenever I talk.")
    }

    private suspend fun Dialogue.toldToShutUp() {
        chatNpc(sad, "Me wish generals wouldn't tell me to shut up.")
        chatNpc(confused, "Sometimes they tell me to shut up when me not even say anything!")
    }

    private suspend fun Dialogue.shoutedDown(general: String) {
        val name = if (general == WARTFACE) "General Wartface" else "General Bentnoze"
        chatNpcSpecific(name, general, angry, "Shut up Grubfoot!")
    }

    private companion object {
        private const val COMPLAINTS = 5
        private const val WARTFACE = "npc.general_wartface_green"
        private const val BENTNOZE = "npc.general_bentnoze_red"
    }
}
