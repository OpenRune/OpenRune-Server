package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class YoungllefDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in FORMS) {
            onPetOp(npc, "Talk-to") { talk(it) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(confused, "I don't get it... Are you real or not?")
                    chatNpc(neutral, "I'm a crystalline formation, made by the elves.")
                    chatPlayer(quiz, "But, like... Can you feel it if I pinch you?")
                    chatNpc(angry, "Don't you even think about it.")
                }
                else -> {
                    chatPlayer(quiz, "What actually are you? A big wolf or something?")
                    chatNpc(neutral, "I suppose I might look something like that.")
                    chatPlayer(quiz, "That sounds like a no. What are you then?")
                    chatNpc(neutral, "A hunllef.")
                    chatPlayer(confused, "A what?")
                    chatNpc(bored, "Nevermind.")
                    chatPlayer(neutral, "You know, you can be a real nightmare sometimes.")
                }
            }
        }

    private companion object {
        val FORMS = listOf("npc.gauntlet_pet", "npc.gauntlet_pet_corrupt")
    }
}
