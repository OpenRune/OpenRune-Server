package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NexlingDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatNpc(angry, "Flood my lungs with blood!")
                    chatPlayer(confused, "Now that just sounds plain unhealthy.")
                    chatNpc(neutral, "You're not meant to take it literally.")
                    chatPlayer(neutral, "Oh, fair enough.")
                    chatNpc(angry, "Fill my soul with smoke!")
                }
                else -> {
                    chatNpc(angry, "Fumus! Umbra! Cruor! Glacies! Don't fail me!")
                    chatPlayer(confused, "Err...")
                    chatNpc(worried, "Fumus? Where is Fumus?")
                    chatPlayer(neutral, "You know they're not here, right?")
                    chatNpc(sad, "But... Cruor?")
                }
            }
        }

    private companion object {
        const val NPC = "npc.nex_pet"
    }
}
