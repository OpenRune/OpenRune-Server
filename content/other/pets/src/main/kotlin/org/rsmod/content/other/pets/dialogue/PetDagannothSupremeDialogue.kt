package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetDagannothSupremeDialogue @Inject constructor(private val followers: PetFollowers) : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            if (followers.isFollowerOf(npc, player)) {
                chatPlayer(neutral, "Hey, so err... I kind of own you now.")
                chatNpc(
                    angry,
                    "Tsssk. Next time you enter those caves, human, my father will be having words.",
                )
                chatPlayer(shifty, "Maybe next time I'll add your brothers to my collection.")
            } else {
                chatPlayer(neutral, "Hey, so err... you're kind of a pet now.")
                chatNpc(
                    angry,
                    "Tsssk. Next time my owner enters those caves, human, my father will be having words.",
                )
                chatPlayer(shifty, "Maybe next time your brothers will join the collection.")
            }
        }

    private companion object {
        const val NPC = "npc.supreme_pet"
    }
}
