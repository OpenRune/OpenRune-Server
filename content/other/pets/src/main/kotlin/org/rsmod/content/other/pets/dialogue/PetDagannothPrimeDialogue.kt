package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.PetFollowers
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetDagannothPrimeDialogue @Inject constructor(private val followers: PetFollowers) : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So despite there being three kings, you're clearly the leader, right?")
            if (followers.isFollowerOf(npc, player)) {
                chatNpc(neutral, "Definitely.")
                chatPlayer(happy, "I'm glad I got you as a pet.")
                chatNpc(angry, "Ugh. Human, I'm not a pet.")
                chatPlayer(neutral, "Stop following me then.")
                chatNpc(sad, "I can't seem to stop.")
            } else {
                chatNpc(neutral, "Definitely")
                chatPlayer(happy, "You make a great pet.")
                chatNpc(angry, "Ugh. Human, I'm not a pet.")
                chatPlayer(neutral, "So fight me then.")
                chatNpc(sad, "I can't seem to do it.")
            }
            chatPlayer(laugh, "Pet.")
        }

    private companion object {
        const val NPC = "npc.prime_pet"
    }
}
