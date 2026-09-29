package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DomDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(quiz, "How long have you been living in those ruins?")
                    chatNpc(neutral, "Not long enough.")
                    chatPlayer(quiz, "Would you like to go back?")
                    chatNpc(neutral, "No. There's more to explore. More to conquer.")
                    chatPlayer(worried, "When you say conquer, I'm on your team, right?")
                    chatNpc(shifty, "For now.")
                    chatPlayer(
                        worried,
                        "That does not fill me with confidence... I hope you don't get any bigger.",
                    )
                }
                else -> {
                    chatPlayer(quiz, "Dom? Is that short for Dominic?")
                    chatNpc(neutral, "Doom of Mokhaiotl.")
                    chatPlayer(shocked, "Oh, I wasn't expecting that.")
                    chatNpc(shifty, "Neither were the Old Ones.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.dom_pet"
    }
}
