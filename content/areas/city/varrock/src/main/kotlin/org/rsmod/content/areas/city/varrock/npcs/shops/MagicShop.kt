package org.rsmod.content.areas.city.varrock.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.hasAtLeast99s
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.runecraftingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class MagicShop @Inject constructor(private val shops: Shops) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1("npc.aubury") { shopDialogue(it.npc) }
        onOpNpc3("npc.aubury") { player.openMagicShop(it.npc) }
    }

    private fun Player.openMagicShop(npc: Npc) {
        val shopInv = if (this.runecraftingLvl == 99) {
            "inv.runeshop_skillcape"
        } else if (this.hasAtLeast99s(2)) {
            "inv.runeshop_skillcape_trimmed"
        } else {
            "inv.runeshop"
        }

        shops.open(this, npc, "Aubury's Rune Shop", shopInv)
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Do you want to buy some runes?")

        val choice = choice3(
            "Can you tell me about your cape?", 1,
            "Yes please!", 2,
            "Oh, it's a rune shop. No thank you, then.", 3,
        )

        when (choice) {
            1 -> skillcape(npc)
            2 -> player.openMagicShop(npc)
            3 -> {
                chatPlayer(neutral, "No thank you.")
                chatNpc(
                    neutral,
                    "Well, if you find someone who does want runes, please send them my way.",
                )
            }
        }
    }

    private suspend fun Dialogue.skillcape(npc: Npc) {
        chatNpc(
            happy,
            "Certainly! Skillcapes are a symbol of achievement. " +
                "Only people who have mastered a skill and reached level 99 can get their hands " +
                "on them and gain the benefits they carry.",
        )
        chatNpc(
            happy,
            "The Cape of Runecrafting has been upgraded with each talisman, allowing you " +
                "to access all Runecrafting altars. Is there anything else I can help you with?",
        )

        val choice = choice2(
            "I'd like to view your store please.", 1,
            "No thank you.", 2,
        )

        when (choice) {
            1 -> player.openMagicShop(npc)
            2 -> {
                chatPlayer(neutral, "No thank you.")
                chatNpc(
                    neutral,
                    "Well, if you find someone who does want runes, please send them my way.",
                )
            }
        }
    }
}
