package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ButchDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(happy, "How's it going, Butch?")
                    chatNpcNoAnim("...")
                    chatPlayer(confused, "Ah... How could I forget... You don't have a head!")
                    chatNpcNoAnim("...")
                    chatPlayer(happy, "Shall we head off on an adventure together?")
                    chatPlayer(worried, "Sorry, poor choice of words.")
                    chatNpcNoAnim("...")
                }
                else -> {
                    chatPlayer(quiz, "Can you speak?")
                    chatNpcNoAnim("Yes...")
                    chatPlayer(confused, "Er... Who's speaking? Is this Butch, or something else?")
                    chatNpcNoAnim("Yes...")
                    chatPlayer(worried, "That's unsettling.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.vardorvis_pet"
    }
}
