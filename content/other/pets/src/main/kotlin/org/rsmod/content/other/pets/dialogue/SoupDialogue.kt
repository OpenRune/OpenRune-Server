package org.rsmod.content.other.pets.dialogue

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.table.fishing.FishingSpotRow
import org.rsmod.content.other.pets.onPetOp
import org.rsmod.content.other.pets.onPetOpU
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class SoupDialogue @Inject constructor() : PluginScript() {
    private var cachedRawFish: Set<String>? = null

    override fun ScriptContext.startup() {
        onPetOp(NPC, "Talk-to") { talk(it) }
        onPetOpU(NPC) { useItem(it.npc, it.objType) }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) =
        startDialogue(npc) {
            when (access.random.of(3)) {
                0 -> {
                    chatPlayer(quiz, "Hey, Soup. Why did you want to follow me?")
                    chatNpc(happy, "Sometimes it's nice to stretch the flippers and dry them out.")
                    chatPlayer(quiz, "Can't you do that alone?")
                    chatNpc(
                        worried,
                        "I wanted to follow someone who looked like they wouldn't let me be turned " +
                            "into Soup soup.",
                    )
                }
                1 -> {
                    chatPlayer(quiz, "Hey, Soup. Where did your name come from?")
                    chatNpc(happy, "Soup is my favourite food.")
                    chatPlayer(quiz, "Any soup in particular?")
                    chatNpc(neutral, "When you live in salt water, all food is soup.")
                    chatPlayer(confused, "So your favourite food is... food?")
                    chatNpc(happy, "Yes, pretty much.")
                }
                else -> {
                    chatPlayer(quiz, "How are you feeling, Soup?")
                    chatNpc(happy, "Souper.")
                }
            }
        }

    private suspend fun ProtectedAccess.useItem(npc: Npc, obj: ItemServerType) {
        val name = obj.name
        when {
            name.contains("sea turtle", ignoreCase = true) -> {
                val kind = when {
                    name.startsWith("Raw", ignoreCase = true) -> "raw"
                    name.startsWith("Burnt", ignoreCase = true) -> "burnt"
                    else -> "cooked"
                }
                startDialogue(npc) {
                    chatNpc(sad, "I can't believe you would show me this...")
                    mesbox(
                        "Soup turns away from you and the $kind sea turtle, a single salty tear " +
                            "forming in his eye.",
                    )
                }
            }
            obj.internalName == HOT_WATER ->
                startDialogue(npc) { chatNpc(neutral, "That soup doesn't look very tasty.") }
            isSoupItem(obj) ->
                startDialogue(npc) {
                    chatNpc(quiz, "Can we make this into soup?")
                    if (invTotal(inv, HOT_WATER) > 0) {
                        chatPlayer(happy, "Yes, I have some hot water here.")
                        invDel(inv, HOT_WATER, 1)
                        invDel(inv, obj.internalName, 1)
                        mesbox("Soup gobbles up the ${name.lowercase()} soup.")
                    } else {
                        chatPlayer(
                            neutral,
                            "It isn't soup... I could get you some hot water to make it soup?",
                        )
                        chatNpc(happy, "Yes please!")
                    }
                }
            else -> startDialogue(npc) { chatNpc(neutral, "No, thank you. I don't want that.") }
        }
    }

    private fun isSoupItem(obj: ItemServerType): Boolean =
        obj.internalName in rawFish() ||
            obj.name.equals("Stew", ignoreCase = true) ||
            obj.name.equals("Curry", ignoreCase = true) ||
            obj.name.equals("Spicy stew", ignoreCase = true) ||
            obj.name in VEGETABLES

    private fun rawFish(): Set<String> =
        cachedRawFish ?: FishingSpotRow.all()
            .mapTo(hashSetOf()) { it.fish.internalName }.also { cachedRawFish = it }

    private companion object {
        const val NPC = "npc.skillpet_sailing"
        const val HOT_WATER = "obj.bowl_water"
        val VEGETABLES = setOf("Cabbage", "Onion", "Potato", "Tomato", "Sweetcorn", "Cooked sweetcorn")
    }
}
