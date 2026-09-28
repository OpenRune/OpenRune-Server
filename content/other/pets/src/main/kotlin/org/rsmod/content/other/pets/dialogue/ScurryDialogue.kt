package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ScurryDialogue @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOpU(NPC) { useItem(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "What's it like being conjoined?")
                    chatNpc(neutral, "I forget they're there most the time.")
                    chatNpcSpecific("Chewy", NPC, neutral, "Until I need some fur to throw.")
                    chatNpcSpecific("Nibbles", NPC, neutral, "Or I have to cast a bolt of electricity.")
                    chatPlayer(laugh, "You could say they're a pain in your backside.")
                    chatNpc(angry, "They'll be a pain in your backside soon if we don't get fed.")
                }
                1 -> {
                    chatPlayer(quiz, "What do you like to eat?")
                    chatNpcSpecific("Nibbles", NPC, neutral, "Humans.")
                    chatPlayer(shocked, "Humans?")
                    chatNpc(neutral, "Nibbles said cheese. We like to eat cheese.")
                    chatPlayer(happy, "Phew, that was close.")
                    chatNpcSpecific("Nibbles", NPC, neutral, "Humans topped with cheese.")
                    chatPlayer(shocked, "What?")
                    chatNpc(neutral, "We like cheese. Just cheese.")
                    chatPlayer(shifty, "I'm going to keep an eye on you. You've got a bad rattitude.")
                    chatNpcSpecific("Chewy", NPC, quiz, "Got any cheese?")
                    chatPlayer(
                        neutral,
                        "Maybe I'll give you some cheese if you're a good rat and don't eat me.",
                    )
                    chatNpc(neutral, "Cheese.")
                    chatPlayer(confused, "Cheese?")
                    chatNpcSpecific("Nibbles", NPC, neutral, "That means deal in rat speak.")
                    chatPlayer(happy, "Thanks Nibbles. Cheese!")
                }
                else -> {
                    chatPlayer(happy, "Scurry!")
                    chatNpc(happy, "${player.displayName}!")
                    chatNpcSpecific("Chewy", NPC, happy, "Nibbles!")
                    chatNpcSpecific("Nibbles", NPC, happy, "Chewy!")
                    chatPlayer(happy, "I hope you like our adventures together.")
                    chatNpc(happy, "We'll have quite the tail to tell...")
                    chatNpc(sad, "... as long as people stop calling me Scuwu.")
                }
            }
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        when {
            obj.name.equals("Cheese", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(happy, "Mmmmm, cheeeeeeeeese.")
                    chatPlayer(neutral, "More cheese? We're gonna need a bigger block...")
                    mesbox("Scurry takes the pieces of cheese.")
                }
            obj.name.equals("Cannon barrels", ignoreCase = true) ->
                startDialogue(npc) {
                    chatNpc(quiz, "What are you trying to do?")
                    mesbox("Scurry runs back several tiles.")
                    chatPlayer(confused, "Nothing... where are you going?")
                    chatNpc(
                        angry,
                        "There's no way I am carrying that on my back, I already have these two " +
                            "weighing me down.",
                    )
                    chatNpcSpecific("Nibbles", NPC, angry, "Oi, we barely weigh anything!")
                    chatNpcSpecific("Chewy", NPC, happy, "Put it on his head like a hat.")
                    chatPlayer(neutral, "Maybe now's not the moment for cannon barrels...")
                }
        }
    }

    private companion object {
        const val NPC = "npc.scurrius_pet"
    }
}
