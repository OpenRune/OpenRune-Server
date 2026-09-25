package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MaggotMarquessDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(happy, "How are you feeling today, maggot marquess?")
            chatNpc(sad, "Bleugh...")
            chatPlayer(confused, "I'm not sure I catch your drift...")
            chatNpc(verymad, "Bleeeeeeeughhhhhhh!!!")
            chatPlayer(shocked, "Maggot marquess! That's disgusting! You've covered my shoes in vomit!")
            chatNpc(neutral, "Bleugh.")
        }

    private companion object {
        const val NPC = "npc.maggot_king_pet"
    }
}
