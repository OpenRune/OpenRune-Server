package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.PetMorphs
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PhoenixDialogue @Inject constructor(private val morphs: PetMorphs) : PluginScript() {
    override fun ScriptContext.startup() {
        for (npc in NPCS) {
            onPetOp(npc, "Talk-to") { talk(it) }
            onPetOpU(npc) { useItem(it.npc, it.objType) }
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(4)) {
                0 -> {
                    chatPlayer(quiz, "So... The Pyromancers, they're cool, right?")
                    chatNpc(neutral, "We share a common goal.")
                    chatPlayer(quiz, "Which is?")
                    chatNpc(
                        neutral,
                        "Keeping the cinders burning and preventing the long night from swallowing us all.",
                    )
                    chatPlayer(worried, "That sounds scary.")
                    chatNpc(neutral, "As long as we remain vigilant and praise the Sun, all will be well.")
                }
                1 -> {
                    chatNpc(silent, "...")
                    chatPlayer(quiz, "... What are you staring at?")
                    chatNpc(neutral, "The great Sol Supra.")
                    chatPlayer(quiz, "Is that me?")
                    chatNpc(neutral, "No mortal. The Sun, as you would say.")
                    chatPlayer(quiz, "Do you worship it?")
                    chatNpc(happy, "It is wonderous... If only I could be so grossly incandescent.")
                }
                2 -> {
                    chatPlayer(happy, "Who's a pretty birdy?")
                    mesbox("The phoenix gives you a smouldering look.")
                }
                else -> {
                    chatNpc(neutral, "One day I will burn so hot I'll become Sacred Ash.")
                    chatPlayer(sad, "Aww, but you're so rare, where would I find another?")
                    chatNpc(neutral, "Do not fret mortal, I will rise from the Sacred Ash greater than ever before.")
                    chatPlayer(quiz, "So you're immortal?")
                    chatNpc(neutral, "As long as the Sun in the sky gives me strength.")
                    chatPlayer(confused, "...Sky?")
                }
            }
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        if (morphs.tryUse(this, npc, obj)) {
            return
        }
        if (obj.internalName == BUCKET_OF_WATER) {
            startDialogue(npc) { mesbox("The phoenix dodges the water.") }
        }
    }

    private companion object {
        const val BUCKET_OF_WATER = "obj.bucket_water"
        val NPCS =
            listOf(
                "npc.phoenix_pet",
                "npc.phoenix_pet_blue",
                "npc.phoenix_pet_green",
                "npc.phoenix_pet_purple",
                "npc.phoenix_pet_white",
            )
    }
}
