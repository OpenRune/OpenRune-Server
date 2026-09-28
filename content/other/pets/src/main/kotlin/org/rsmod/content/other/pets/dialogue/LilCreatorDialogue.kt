package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LilCreatorDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(CREATOR, "Talk-to") { talkCreator(it) }
        onPetOp(DESTRUCTOR, "Talk-to") { talkDestructor(it) }
    }

    private suspend fun ProtectedAccess.talkCreator(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(
                quiz,
                "So why are you fighting over that obelisk anyway? Just looks like a bit of old " +
                    "rock to me.",
            )
            chatNpc(angry, "Puny human! You wouldn't understand.")
            chatPlayer(neutral, "Yeah, I'm the puny one.")
        }

    private suspend fun ProtectedAccess.talkDestructor(npc: Npc) =
        startDialogue(npc) {
            chatPlayer(quiz, "Don't you get hot standing around pools of lava all day?")
            chatNpc(angry, "I care not for equable temperature... only destruction!")
            chatPlayer(neutral, "Each to their own, I suppose...")
        }

    private companion object {
        const val CREATOR = "npc.soulwars_pet_blue"
        const val DESTRUCTOR = "npc.soulwars_pet_red"
    }
}
