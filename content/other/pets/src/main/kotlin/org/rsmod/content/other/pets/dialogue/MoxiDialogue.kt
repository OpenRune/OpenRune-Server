package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MoxiDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatNpc(happy, "Euat ami! Euat chua tepat!")
                    chatPlayer(confused, "Uh huh...")
                    chatNpc(happy, "Zema ami iknil! Zema lini euat tal miki!")
                    chatPlayer(neutral, "If you say so...")
                    chatNpc(happy, "Zema ikam!")
                }
                else -> {
                    chatNpc(happy, "Zema ates! Euat achi!")
                    chatPlayer(neutral, "I'm listening, honest.")
                    chatNpc(happy, "Euat tepat antil! Euat tepat kuhu! Euat rani!")
                    chatPlayer(confused, "That's very interesting...")
                    chatNpc(happy, "Tepat!")
                }
            }
        }

    private companion object {
        const val NPC = "npc.amoxliatl_pet"
    }
}
