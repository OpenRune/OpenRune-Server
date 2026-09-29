package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class TinyTemporDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(laugh, "Something smells a bit fishy around here, doesn't it?")
                    chatNpc(bored, "Very creative.")
                    chatPlayer(laugh, "You've met your nemosis.")
                    chatNpc(neutral, "Cod you not?")
                    chatPlayer(happy, "That's the spirit. I'd give that a 9 on a scale of 1 to 10.")
                    chatNpc(happy, "Carp, seems like I'm alrody hooked.")
                    chatPlayer(
                        laugh,
                        "Whale, you don't need to be a brain sturgeon to come up with these lines.",
                    )
                    chatNpc(laugh, "Oh my, that one krilled me.")
                }
                1 -> {
                    chatPlayer(happy, "Hey! Tell me something I don't know.")
                    chatNpc(neutral, "Seasons come and go, but I will never change.")
                    chatPlayer(confused, "What is that supposed to mean?")
                    chatNpc(angry, "I am the Spirit of the Sea, not a writer. No need to overanalyse!")
                    chatPlayer(neutral, "I'm sorry, you're right. Writing with those arms can't be easy anyway.")
                    chatNpc(angry, "Don't push it, human.")
                }
                else -> {
                    chatPlayer(happy, "Tiny Tempor, use Splash!")
                    chatNpc(confused, "Excuse me?")
                    chatPlayer(neutral, "Never mind, it won't be very effective anyway.")
                    chatNpc(bored, "You humans will forever remain a mystery to me.")
                }
            }
        }

    private companion object {
        const val NPC = "npc.tempoross_pet"
    }
}
