package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BabyChinchompaDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in STANDARD_FORMS) {
            onPetOp(npc, "Talk-to") { talk(it) }
        }
        onPetOp(GOLD, "Talk-to") { talkGold(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) { chatNpc(happy, "Squeak squeak!") }

    private suspend fun ProtectedAccess.talkGold(npc: Npc) =
        startDialogue(npc) { chatNpc(happy, "Squeaka squeaka!") }

    private companion object {
        val STANDARD_FORMS =
            listOf(
                "npc.skillpet_hunter_grey",
                "npc.skillpet_hunter_red",
                "npc.skillpet_hunter_black",
            )
        const val GOLD = "npc.skillpet_hunter_gold"
    }
}
