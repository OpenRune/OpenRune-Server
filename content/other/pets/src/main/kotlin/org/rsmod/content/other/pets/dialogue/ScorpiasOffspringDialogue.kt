package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScorpiasOffspringDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(
                quiz,
                "At night time, if I were to hold ultraviolet light over you, would you glow?",
            )
            chatNpc(neutral, "Two things wrong there, human.")
            chatPlayer(quiz, "Oh?")
            chatNpc(neutral, "One, When has it ever been night time here?")
            chatNpc(neutral, "Two, When have you ever seen ultraviolet light around here?")
            chatPlayer(confused, "Hm...")
            chatNpc(happy, "In answer to your question though. Yes I, like every scorpion, would glow.")
        }

    private companion object {
        const val NPC = "npc.scorpiapet"
    }
}
