package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BabyMoleDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOp(NPC_NAKED, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(happy, "Hey, Mole. How is life above ground?")
            chatNpc(
                neutral,
                "Well, the last time I was above ground, I was having to contend with people " +
                    "throwing snow at some weird yellow duck in my park.",
            )
            chatPlayer(quiz, "Why were they doing that?")
            chatNpc(
                neutral,
                "No idea, I didn't stop to ask as an angry mob was closing in on them pretty quickly.",
            )
            chatPlayer(sad, "Sounds awful.")
            chatNpc(happy, "Anyway, keep Molin'!")
        }

    private companion object {
        const val NPC = "npc.mole_pet"
        const val NPC_NAKED = "npc.mole_pet_naked"
    }
}
