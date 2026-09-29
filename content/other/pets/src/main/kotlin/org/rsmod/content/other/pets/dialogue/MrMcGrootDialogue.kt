package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MrMcGrootDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(2)) {
                0 -> {
                    chatPlayer(happy, "Mr McGroot! Wonderful to see you!")
                    chatNpc(quiz, "Got any snacks? I'll eat anything.")
                    chatPlayer(sad, "None of my items are goat snacks, sorry.")
                }
                else -> {
                    chatPlayer(quiz, "So is there a Mrs McGroot?")
                    chatNpc(
                        happy,
                        "Yes! Haven't seen her for a while though. She popped into a spiky hole to " +
                            "check it out.",
                    )
                    chatPlayer(worried, "I'm sure she'll be back soon...")
                }
            }
        }

    private companion object {
        const val NPC = "npc.goat_pit_pet"
    }
}
