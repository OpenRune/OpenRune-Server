package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetGeneralGraardorDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(bored, "Not sure this is going to be worth my time but... how are you?")
            chatNpc(angry, "SFudghoigdfpDSOPGnbSOBNfdbdnopbdnopbddfnopdfpofhdb ARRRGGGGH")
            chatPlayer(neutral, "Nope. Not worth it.")
        }

    private companion object {
        const val NPC = "npc.bandos_pet"
    }
}
