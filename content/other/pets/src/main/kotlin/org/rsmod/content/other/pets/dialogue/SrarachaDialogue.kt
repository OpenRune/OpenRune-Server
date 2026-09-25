package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.PetMorphs
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SrarachaDialogue @Inject constructor(private val morphs: PetMorphs) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in FORMS) {
            onPetOp(npc, "Talk-to") { talk(it) }
            onPetOpU(npc) { useItem(it.npc, it.objType) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "So what kind of spider are you...?")
            chatNpc(shocked, "The hive cluster is under attack!")
            chatPlayer(neutral, "Erm, I think the attack is over. I have already killed your queen.")
            chatNpc(angry, "Then we should spawn more overlords!")
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        if (morphs.tryUse(this, npc, obj)) {
            return
        }
        if (obj.name.equals("Newspaper", ignoreCase = true)) {
            startDialogue(npc) { chatNpc(angry, "Don't you dare!") }
        }
    }

    private companion object {
        val FORMS = listOf("npc.sarachnispet", "npc.sarachnispet_blue", "npc.sarachnispet_orange")
    }
}
