package org.rsmod.content.areas.city.varrock.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FineClothesShop @Inject constructor(private val shops: Shops) : PluginScript() {
    // TODO: More stuff needs implementing for Thessalia

    override fun ScriptContext.startup() {
        onOpNpc1("npc.thessalia") { shopDialogue(it.npc) }
        onOpNpc3("npc.thessalia") { player.openFineClothesShop(it.npc) }
    }

    private fun Player.openFineClothesShop(npc: Npc) {
        shops.open(this, npc, "Thessalia's Fine Clothes", "inv.clotheshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Do you want to buy any fine clothes?")


        val choice = choice3(
            "What have you got?", 1,
            "I'd just like to buy some clothes.", 2,
            "No, thank you.", 3,
        )

        when (choice) {
            1 -> {
                chatPlayer(happy, "What have you got?")
                player.openFineClothesShop(npc)
            }

            2 -> {
                chatPlayer(happy, "I'd just like to buy some clothes.")
                player.openFineClothesShop(npc)
            }

            3 -> {
                chatPlayer(neutral, "No, thank you.")
                chatNpc(neutral, "Well, please return if you change your mind.")
            }
        }
    }
}
