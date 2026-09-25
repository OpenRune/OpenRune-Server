package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetZilyanaDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) {
        val hasGodsword = invTotal(inv, GODSWORD) > 0 || GODSWORD in worn
        startDialogue(npc) {
            if (hasGodsword) {
                chatNpc(verymad, "FIND THE GODSWORD!")
                chatPlayer(happy, "I FOUND THE GODSWORD!")
                chatNpc(happy, "GOOD!!!!!")
                return@startDialogue
            }
            repeat(10) {
                chatNpc(verymad, "FIND THE GODSWORD!")
                chatPlayer(verymad, "FIND THE GODSWORD!")
            }
            chatNpc(verymad, "FIND THE GODSWORD!")
        }
    }

    private companion object {
        const val NPC = "npc.saradomin_pet"
        const val GODSWORD = "obj.sgs"
    }
}
