package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SmolcanoDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(quiz, "How much did you pay for your ring of stone?")
                    chatNpc(confused, "What are you talking about?")
                    chatPlayer(happy, "They're so expensive, but so much fun!")
                    chatNpc(bored, "Right...")
                }
                else -> {
                    chatPlayer(quiz, "So why do they call you Zalcano?")
                    chatNpc(neutral, "Well....")
                    chatPlayer(happy, "Is it because you're like a vol-")
                    chatNpc(angry, "Don't say it!")
                }
            }
        }

    private companion object {
        const val NPC = "npc.zalcano_pet"
    }
}
