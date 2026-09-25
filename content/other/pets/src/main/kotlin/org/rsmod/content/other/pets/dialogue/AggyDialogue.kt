package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AggyDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "Hey, Aggy, why are you following me? You don't seem happy about it.")
                    chatNpc(angry, "I'm not following you. We are walking in the same direction. Get over yourself.")
                }
                1 -> {
                    chatPlayer(quiz, "Hey, Aggy, what are you made out of?")
                    chatNpc(angry, "Sunstone and hate.")
                }
                else -> {
                    chatNpc(angry, "Don't touch me!")
                    chatPlayer(confused, "I was just going to ask you...")
                    chatNpc(angry, "Don't ask me!")
                }
            }
        }

    private companion object {
        const val NPC = "npc.mad_angel_pet"
    }
}
