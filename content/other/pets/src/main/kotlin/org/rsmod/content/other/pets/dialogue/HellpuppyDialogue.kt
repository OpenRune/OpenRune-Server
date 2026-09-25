package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class HellpuppyDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(5)) {
                0 -> {
                    chatPlayer(quiz, "How many souls have you devoured?")
                    chatNpc(neutral, "None.")
                    chatPlayer(happy, "Awww p-")
                    chatNpc(shifty, "Yet.")
                    chatPlayer(worried, "Oh.")
                }
                1 -> {
                    chatPlayer(happy, "What a cute puppy, how nice to meet you.")
                    chatNpc(shifty, "It'll be nice to meat you too...")
                    chatPlayer(worried, "Urk... nice doggy.")
                    chatNpc(angry, "Grrrr....")
                }
                2 -> {
                    chatPlayer(happy, "Hell yeah! Such a cute puppy!")
                    chatNpc(angry, "Silence mortal! Or I'll eat your soul.")
                    chatPlayer(quiz, "Would that go well with lemon?")
                    chatNpc(angry, "Grrrr....")
                }
                3 -> {
                    chatPlayer(happy, "Why were the hot dogs shivering?")
                    chatNpc(angry, "Grrrrr...")
                    chatPlayer(happy, "Because they were served-")
                    chatNpc(verymad, "GRRRRRR...")
                    chatPlayer(worried, "-with... chilli?")
                }
                else -> {
                    chatPlayer(
                        quiz,
                        "I wonder if I need to invest in a trowel when I take you out for a walk.",
                    )
                    chatNpc(neutral, "More like a shovel.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.hellpet"
    }
}
