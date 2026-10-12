package org.rsmod.content.areas.city.alkharid.npcs.shops

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

// https://oldschool.runescape.wiki/w/Transcript:Dommik
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:rogue_trader] "What do you know about Ali Morrisane?"
class CraftingShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dommik") { shopDialogue(it.npc) }
        onOpNpc3("npc.dommik") { player.openCraftingShop(it.npc) }
    }

    private fun Player.openCraftingShop(npc: Npc) {
        shops.open(this, npc, "Dommik's Crafting Store.", "inv.craftingshop2")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Would you like to buy some crafting equipment?")
        val option =
            choice2(
                "No thanks; I've got all the Crafting equipment I need.",
                1,
                "Let's see what you've got, then.",
                2,
            )
        when (option) {
            1 -> {
                chatPlayer(neutral, "No thanks; I've got all the Crafting equipment I need.")
                chatNpc(happy, "Okay. Fare well on your travels.")
            }
            2 -> player.openCraftingShop(npc)
        }
    }
}
