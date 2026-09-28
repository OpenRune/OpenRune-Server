package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PrinceBlackDragonDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Shouldn't a prince only have two heads?")
            chatNpc(quiz, "Why is that?")
            chatPlayer(
                neutral,
                "Well, a standard Black dragon has one, the King has three so inbetween must have two?",
            )
            chatNpc(bored, "You're overthinking this.")
        }

    private companion object {
        const val NPC = "npc.kbd_pet"
    }
}
