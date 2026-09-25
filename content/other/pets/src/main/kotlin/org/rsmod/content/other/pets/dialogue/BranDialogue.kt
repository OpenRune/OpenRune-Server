package org.rsmod.content.other.pets.dialogue

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BranDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(BRAN, "Talk-to") { talkBran(it) }
        onPetOp(RIC, "Talk-to") { talkRic(it) }
    }

    private suspend fun ProtectedAccess.talkBran(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "Anywhere you want to go?")
                    chatNpc(neutral, "Home.")
                    chatPlayer(neutral, "Maybe another day.")
                }
                1 -> {
                    chatPlayer(quiz, "How is the weather down there?")
                    chatNpc(neutral, "Hot.")
                    chatPlayer(sad, "Ah, I'm sorry to hear that.")
                    chatNpc(happy, "No, I like.")
                    chatPlayer(neutral, "Oh right yeah, that makes sense.")
                }
                else -> {
                    chatPlayer(quiz, "If I get into a fight, are you going to help me?")
                    chatNpc(neutral, "I follow.")
                    chatPlayer(quiz, "I'll take that as a yes, then?")
                    chatNpc(neutral, "Just follow.")
                    chatPlayer(neutral, "I'll take that as a no, then.")
                    chatNpc(neutral, "Yes.")
                    chatPlayer(confused, "Wait, yes? You'll fight?")
                    chatNpc(neutral, "No.")
                    chatPlayer(neutral, "Cool.")
                    chatNpc(neutral, "Hot.")
                    chatPlayer(confused, "What?")
                    chatNpc(neutral, "...")
                    chatPlayer(neutral, "...")
                }
            }
        }

    private suspend fun ProtectedAccess.talkRic(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "Anywhere you want to go?")
                    chatNpc(neutral, "Home.")
                    chatPlayer(neutral, "Maybe another day.")
                }
                1 -> {
                    chatPlayer(quiz, "How is the weather down there?")
                    chatNpc(neutral, "Cold.")
                    chatPlayer(sad, "Ah, I'm sorry to hear that.")
                    chatNpc(happy, "No, I like.")
                    chatPlayer(neutral, "Oh right yeah, that makes sense.")
                }
                else -> {
                    chatPlayer(quiz, "If I get into a fight, are you going to help me?")
                    chatNpc(neutral, "I follow.")
                    chatPlayer(quiz, "I'll take that as a yes, then?")
                    chatNpc(neutral, "Just follow.")
                    chatPlayer(neutral, "I'll take that as a no, then.")
                    chatNpc(neutral, "Yes.")
                    chatPlayer(confused, "Wait, yes? You'll fight?")
                    chatNpc(neutral, "No.")
                    chatPlayer(neutral, "Cool.")
                    chatNpc(neutral, "Cool.")
                }
            }
        }

    private companion object {
        const val BRAN = "npc.rtbranda_pet"
        const val RIC = "npc.rteldric_pet"
    }
}
