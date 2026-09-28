package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class KalphitePrincessDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(WALKING, "Talk-to") { talk(it) }
        onPetOp(FLYING, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "What is it with your kind and potato cactus?")
            chatNpc(quiz, "Truthfully?")
            chatPlayer(happy, "Yeah, please.")
            chatNpc(neutral, "Soup. We make a fine soup with it.")
            chatPlayer(confused, "Kalphites can cook?")
            chatNpc(
                shifty,
                "Nah, we just collect it and put it there because we know fools like yourself will " +
                    "come down looking for it then inevitably be killed by my mother.",
            )
            chatPlayer(happy, "Evidently not, that's how I got you!")
            chatNpc(neutral, "Touché.")
        }

    private companion object {
        const val WALKING = "npc.kq_pet_walking"
        const val FLYING = "npc.kq_pet_flying"
    }
}
