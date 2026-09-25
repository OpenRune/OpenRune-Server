package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class QuetzinDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatNpc(quiz, "Can I bother you for a moment?")
            chatPlayer(quiz, "Oh yes?")
            chatNpc(
                worried,
                "Do you ever think you'd rather have one of my larger friends to be your " +
                    "companion? I can't carry you like they can.",
            )
            chatPlayer(
                happy,
                "I'm actually glad to have you with me! You may even grow up to be giant one day. " +
                    "Maybe then you could carry me everywhere.",
            )
            chatNpc(neutral, "Except towards volcanoes... We birds aren't so good in those conditions.")
        }

    private companion object {
        const val NPC = "npc.quetzal_pet"
    }
}
