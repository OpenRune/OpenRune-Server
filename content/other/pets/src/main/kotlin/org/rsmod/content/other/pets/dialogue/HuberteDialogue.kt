package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HuberteDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatNpc(happy, "Caw!")
                    chatPlayer(quiz, "Are you a bird?")
                    chatNpc(angry, "Roar!")
                    chatPlayer(quiz, "A dragon?")
                    chatNpc(shifty, "Hiss!")
                    chatPlayer(quiz, "A snake?")
                    chatNpc(happy, "Roar! Caw! Hiss!")
                    chatPlayer(confused, "A dragon snake bird?")
                    chatNpc(neutral, "...")
                }
                else -> {
                    chatNpc(happy, "*bird noises*")
                    chatPlayer(happy, "Aren't you a cute litle thing.")
                    chatNpc(verymad, "*screeches*")
                    chatPlayer(shocked, "My ears!")
                    chatNpc(happy, "*bird noises*")
                }
            }
        }

    private companion object {
        const val NPC = "npc.huey_pet"
    }
}
