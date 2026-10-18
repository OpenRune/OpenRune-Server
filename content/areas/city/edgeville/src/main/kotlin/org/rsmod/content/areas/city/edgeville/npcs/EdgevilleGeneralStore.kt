package org.rsmod.content.areas.city.edgeville.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EdgevilleGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (staff in STAFF) {
            onOpNpc1(staff) { shopDialogue(it.npc) }
            onOpNpc3(staff) { player.openGeneralStore(it.npc) }
        }
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) {
        startDialogue(npc) {
            chatNpc(happy, "Can I help you at all?")
            if (choice2("Yes please. What are you selling?", true, "No thanks.", false)) {
                player.openGeneralStore(npc)
                return@startDialogue
            }
            chatPlayer(neutral, "No thanks.")
        }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Edgeville General Store", "inv.generalshop5")
    }

    private companion object {
        private val STAFF = listOf("npc.generalshopkeeper5", "npc.generalassistant5")
    }
}
