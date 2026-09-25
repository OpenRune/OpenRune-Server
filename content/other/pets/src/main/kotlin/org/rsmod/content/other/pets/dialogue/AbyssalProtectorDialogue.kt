package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class AbyssalProtectorDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatNpc(quiz, "Do you know what happens when you stare into the Abyss?")
                    chatPlayer(quiz, "What?")
                    chatNpc(happy, "It stares back!")
                    chatPlayer(neutral, "Right...")
                }
                else -> {
                    chatNpc(angry, "We don't spend enough time in the Abyss!")
                    chatPlayer(quiz, "Well how much time do you think we should be spending there?")
                    chatNpc(happy, "All the time! Eternal time in the eternal Abyss!")
                    chatPlayer(neutral, "Yeah... I don't think that's going to work.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.abyssal_pet"
    }
}
