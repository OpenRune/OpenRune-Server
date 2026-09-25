package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BeefDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(happy, "You are legendairyily cute.")
                    chatNpc(neutral, "moo.")
                    chatPlayer(happy, "I love moo.")
                    chatNpc(neutral, "moo.")
                }
                else -> {
                    chatPlayer(happy, "Hello there!")
                    chatNpc(neutral, "moo.")
                    chatPlayer(quiz, "You doing okay?")
                    chatNpc(neutral, "moo.")
                    chatPlayer(confused, "I can't speak cow.")
                    chatNpc(neutral, "...")
                    chatPlayer(neutral, "Okay, one moo for yes, two moo's for no. Got it?")
                    chatNpc(neutral, "moo.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.cowboss_pet"
    }
}
