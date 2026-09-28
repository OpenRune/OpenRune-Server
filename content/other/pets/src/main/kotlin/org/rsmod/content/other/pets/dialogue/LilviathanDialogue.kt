package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LilviathanDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(quiz, "You okay there, Lil'viathan? You look at little worse for wear.")
                    chatNpc(sad, "*wheeez*")
                    chatPlayer(neutral, "I think you have a smoking problem.")
                }
                else -> {
                    chatPlayer(happy, "Hello, Lil'viathan.")
                    chatNpc(verymad, "ROAAAAARRR!")
                    chatNpc(verymad, "SCREEEEEEE!")
                    chatNpc(verymad, "HSSSSSSSSS!")
                    chatPlayer(neutral, "Good talk.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.leviathan_pet"
    }
}
