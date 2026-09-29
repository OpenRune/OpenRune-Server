package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class VenenatisSpiderlingDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOp(NPC_LEGACY, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(neutral, "It's a damn good thing I don't have arachnophobia.")
            chatNpc(
                neutral,
                "We're misunderstood. Without us in your house, you'd be infested with flies and " +
                    "other REAL nasties.",
            )
            chatPlayer(bored, "Thanks for that enlightening fact.")
            chatNpc(neutral, "Everybody gets one.")
        }

    private companion object {
        const val NPC = "npc.venenatispet"
        const val NPC_LEGACY = "npc.venenatispet_legacy"
    }
}
