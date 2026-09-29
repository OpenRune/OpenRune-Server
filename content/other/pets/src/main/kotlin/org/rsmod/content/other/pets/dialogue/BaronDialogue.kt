package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BaronDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(happy, "You're so cute!")
                    chatNpc(angry, "I will devour everything you know and love!")
                    chatPlayer(worried, "Well then.")
                }
                else -> {
                    chatPlayer(quiz, "You okay, Baron?")
                    chatNpc(angry, "I must grow! I must eat!")
                    chatPlayer(worried, "Keep those little teeth away from me please. I'm keeping an eye on you.")
                    chatNpc(neutral, "And I'm keeping all my eyes on you.")
                    chatPlayer(shocked, "*gulp*")
                }
            }
        }

    private companion object {
        const val NPC = "npc.duke_sucellus_pet"
    }
}
