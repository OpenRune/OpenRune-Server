package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MuphinDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(RANGED, "Talk-to") { talk(it) }
        onPetOp(MELEE, "Talk-to") { talk(it) }
        onPetOp(SHIELDED, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(happy, "Who's the cutest little nightmare-spawn in all of Gielinor?")
                    chatNpc(neutral, "...")
                    chatPlayer(happy, "Is it you?!")
                    chatNpc(neutral, "...")
                    chatPlayer(happy, "Yes, it's you my little Muphin!")
                    chatNpc(angry, "Stop this at once.")
                }
                1 -> {
                    chatPlayer(
                        quiz,
                        "Can you teach me how to do that thing where you teleport around really quickly?",
                    )
                    chatNpc(
                        neutral,
                        "That requires moving between realms... Your simple form would not withstand it.",
                    )
                    chatPlayer(shifty, "You'd be surprised.")
                }
                else -> {
                    chatPlayer(
                        shifty,
                        "You know... if you were bite-sized, I reckon you'd be quite nutritious.",
                    )
                    chatNpc(angry, "Do NOT eat me, human.")
                }
            }
        }

    private companion object {
        const val RANGED = "npc.muspah_pet"
        const val MELEE = "npc.muspah_pet_melee"
        const val SHIELDED = "npc.muspah_pet_shielded"
    }
}
