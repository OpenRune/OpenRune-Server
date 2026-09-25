package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NidDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NID, "Talk-to") { talk(it) }
        onPetOp(RAX, "Talk-to") { talk(it) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "At what age do you become venomous? Can you help me in battle?")
                    chatNpc(neutral, "...")
                    chatPlayer(
                        neutral,
                        "Venom is pretty good at dealing damage, imagine how useful that would be.",
                    )
                    chatNpc(neutral, "...")
                    chatPlayer(
                        happy,
                        "You could just venom something and I can hide behind a wall and watch it die.",
                    )
                    chatNpc(neutral, "...")
                    chatPlayer(sad, "Guess not.")
                }
                1 -> {
                    chatPlayer(quiz, "I've never really thought about it before. Do I need to feed you?")
                    chatNpc(neutral, "...")
                    chatPlayer(
                        confused,
                        "You follow me around a lot and never seem to do anything, nor feed yourself, " +
                            "drink water, even defecate.",
                    )
                    chatNpc(neutral, "...")
                    chatPlayer(quiz, "How are you so low maintenance? It's quite surreal.")
                    chatNpc(neutral, "...")
                    chatPlayer(quiz, "Would you like a sip of a Saradomin brew?")
                    chatNpc(neutral, "...")
                    chatPlayer(neutral, "Well if it aint' broke...")
                }
                else -> {
                    chatPlayer(worried, "Spiders really creep me out.")
                    chatNpc(neutral, "...")
                    chatPlayer(worried, "Like seriously freak me out.")
                    chatNpc(neutral, "...")
                    chatPlayer(angry, "Stop staring at me.")
                    chatNpc(neutral, "....")
                }
            }
        }

    private companion object {
        const val NID = "npc.araxxor_pet"
        const val RAX = "npc.araxxor_pet_cute"
    }
}
