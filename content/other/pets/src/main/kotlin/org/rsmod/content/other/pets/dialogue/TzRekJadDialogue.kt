package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TzRekJadDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOp(NPC_INFERNO, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(quiz, "Do you miss your people?")
                    chatNpc(happy, "Mej-TzTok-Jad Kot-Kl! (TzTok-Jad will protect us!)")
                    chatPlayer(sad, "No.. I don't think so.")
                    chatNpc(worried, "Jal-ZekKl? (Foreigner hurt us?)")
                    chatPlayer(neutral, "No, no, I wouldn't hurt you.")
                }
                else -> {
                    chatPlayer(quiz, "Are you hungry?")
                    chatNpc(happy, "Kl-Kra!")
                    chatPlayer(confused, "Ooookay...")
                }
            }
        }

    private companion object {
        const val NPC = "npc.jadpet"
        const val NPC_INFERNO = "npc.jadpet_inferno"
    }
}
