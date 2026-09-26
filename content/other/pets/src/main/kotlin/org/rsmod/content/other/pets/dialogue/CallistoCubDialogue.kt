package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CallistoCubDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOp(NPC_LEGACY, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(laugh, "Why the grizzly face?")
            chatNpc(angry, "You're not funny...")
            chatPlayer(laugh, "You should get in the.... sun more.")
            chatNpc(angry, "You're really not funny...")
            chatPlayer(laugh, "One second, let me take a picture of you with my.... kodiak camera.")
            chatNpc(angry, ".....")
            chatPlayer(laugh, "Feeling.... blue.")
            chatNpc(verymad, "If you don't stop, I'm going to leave some... brown... at your feet, human.")
        }

    private companion object {
        const val NPC = "npc.callistopet"
        const val NPC_LEGACY = "npc.callistopet_legacy"
    }
}
