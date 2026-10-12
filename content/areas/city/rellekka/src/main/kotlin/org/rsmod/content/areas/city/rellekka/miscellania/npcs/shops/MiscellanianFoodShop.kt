package org.rsmod.content.areas.city.rellekka.miscellania.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Osvald
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MiscellanianFoodShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(OSVALD) { startDialogue(it.npc) { osvald(it.npc) } }
        onOpNpc3(OSVALD) { player.openFoodShop(it.npc) }
    }

    private fun Player.openFoodShop(npc: Npc) {
        shops.open(this, npc, "Miscellanian Food Shop", "inv.royal_foodshop")
    }

    private suspend fun Dialogue.osvald(npc: Npc) {
        chatNpc(happy, "Welcome to the Miscellania food store. We've only opened recently.")
        chatNpc(happy, "Would you like to buy anything, Your Royal Highness?")
        val option =
            choice3(
                "Could you show me what you have for sale?",
                1,
                "No thank you, I don't need food just now.",
                2,
                "What's it like living down here?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Could you show me what you have for sale?")
                player.openFoodShop(npc)
            }
            2 -> chatPlayer(neutral, "No thank you, I don't need food just now.")
            3 -> {
                chatPlayer(quiz, "What's it like living down here?")
                if (QuestRequirements.isOnQuest(player, ROYAL_TROUBLE)) {
                    chatNpc(happy, "The town's growing larger, and business is doing well.")
                    chatNpc(
                        worried,
                        "I'm worried about the stories I've heard about monsters, though.",
                    )
                    chatPlayer(quiz, "What stories about monsters?")
                    chatNpc(
                        worried,
                        "I heard that one of the dwarves saw monsters in the other caves!",
                    )
                    chatNpc(
                        neutral,
                        "I don't know more than that, so you'd have to ask the dwarves.",
                    )
                    return
                }
                chatNpc(happy, "The town's thriving. I'm sure it'll soon be as busy as Rellekka!")
            }
        }
    }

    private companion object {
        const val ROYAL_TROUBLE = "quest_royaltrouble"
        const val OSVALD = "npc.royal_foodshop_owner"
    }
}
