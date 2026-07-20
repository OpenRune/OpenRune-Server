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

class SwordShop @Inject constructor(private val shops: Shops) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1("npc.swordshop1") { shopDialogue(it.npc) }
        onOpNpc3("npc.swordshop1") { player.openSwordShop(it.npc) }
        onOpNpc1("npc.swordshop2") { shopDialogue(it.npc) }
        onOpNpc3("npc.swordshop2") { player.openSwordShop(it.npc) }
    }

    private fun Player.openSwordShop(npc: Npc) {
        shops.open(this, npc, "Varrock Swordshop", "inv.swordshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Hello, bold adventurer! Can I interest you in some swords?")

        val choice = choice2(
            "Yes please!", 1,
            "No, I'm okay for swords right now.", 2,
        )

        when (choice) {
            1 -> player.openSwordShop(npc)
            2 -> {
                chatPlayer(neutral, "No, I'm okay for swords right now.")
                chatNpc(neutral, "Come back if you need any.")
            }
        }
    }

}
