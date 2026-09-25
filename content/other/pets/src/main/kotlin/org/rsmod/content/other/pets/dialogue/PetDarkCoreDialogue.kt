package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PetDarkCoreDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(DARK_CORE, "Talk-to") { talkDarkCore(it) }
        onPetOp(CORPOREAL_CRITTER, "Talk-to") { talkCorporealCritter(it) }
    }

    private suspend fun ProtectedAccess.talkDarkCore(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Got any sigils for me?")
            mesbox("The Core shakes its head.")
            chatPlayer(angry, "Damnit Core-al!")
            chatPlayer(happy, "Let's bounce!")
        }

    private suspend fun ProtectedAccess.talkCorporealCritter(npc: Npc) =
        startDialogue(npc) {
            chatNpc(sad, "I'm hungry!")
            chatPlayer(quiz, "How hungry?")
            chatNpc(sad, "I'm empty to the core!")
            chatPlayer(quiz, "Would an apple do?")
            chatNpc(confused, "What is apple?")
            chatPlayer(happy, "Something where you eat the outside and throw the core away!")
        }

    private companion object {
        const val DARK_CORE = "npc.core_pet"
        const val CORPOREAL_CRITTER = "npc.corp_pet"
    }
}
