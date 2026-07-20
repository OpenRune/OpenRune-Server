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

class ArcheryShop @Inject constructor(private val shops: Shops) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1("npc.lowe") { shopDialogue(it.npc) }
        onOpNpc3("npc.lowe") { player.openArcheryShop(it.npc) }
    }

    private fun Player.openArcheryShop(npc: Npc) {
        shops.open(this, npc, "Lowe's Archery Emporium", "inv.archeryshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Welcome to Lowe's Archery Emporium. Do you want to see my wares?")

        val choice = choice2(
            "Yes please!", 1,
            "No, I prefer to bash things close up.", 2,
        )

        when (choice) {
            1 -> player.openArcheryShop(npc)
            2 -> {
                chatPlayer(neutral, "No, I prefer to bash things close up.")
                chatNpc(neutral, "Humph, philistine.")
            }
        }
    }

}
