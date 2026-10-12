package org.rsmod.content.areas.city.rellekka.miscellania.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Halla
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:royal_trouble] monster rumours while the quest is in progress
class MiscellanianClothesShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(HALLA) { startDialogue(it.npc) { halla(it.npc) } }
        onOpNpc3(HALLA) { player.openClothesShop(it.npc) }
    }

    private fun Player.openClothesShop(npc: Npc) {
        shops.open(this, npc, "Miscellanian Clothes Shop", "inv.royal_clotheshop")
    }

    private suspend fun Dialogue.halla(npc: Npc) {
        chatNpc(
            happy,
            "Welcome to Miscellania's first clothing store! We sell clothing made especially for " +
                "Fremenniks and Dwarves.",
        )
        chatNpc(happy, "Can I help you with anything, Your Royal Highness?")
        val option =
            choice3(
                "I'd like to look at what you have for sale.",
                1,
                "No thank you, I'm fine.",
                2,
                "What's it like living down here?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "I'd like to look at what you have for sale.")
                player.openClothesShop(npc)
            }
            2 -> chatPlayer(neutral, "No thank you, I'm fine.")
            3 -> {
                chatPlayer(quiz, "What's it like living down here?")
                chatNpc(
                    happy,
                    "It's very spacious down here. One of the dwarves said that the caves go on " +
                        "for miles!",
                )
                chatNpc(
                    sad,
                    "The only problem I find is that the lighting's not very good, which means I " +
                        "make mistakes when cutting cloth.",
                )
            }
        }
    }

    private companion object {
        const val HALLA = "npc.royal_clotheshop_owner"
    }
}
