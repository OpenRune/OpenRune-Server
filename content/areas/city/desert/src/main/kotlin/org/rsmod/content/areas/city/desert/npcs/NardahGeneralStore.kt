package org.rsmod.content.areas.city.desert.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class NardahGeneralStore @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (staff in STAFF) {
            onOpNpc1(staff) { startDialogue(it.npc) { generalStore(it.npc) } }
            onOpNpc3(staff) { player.openGeneralStore(it.npc) }
        }
    }

    private fun Player.openGeneralStore(npc: Npc) = shops.open(this, npc, "Nardah General Store.", "inv.generalshopnardah")

    private suspend fun Dialogue.generalStore(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        if (choice2("Yes please. What are you selling?", true, "No thanks.", false)) {
            access.player.openGeneralStore(npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private companion object {
        val STAFF = listOf("npc.elid_general_seller")
    }
}
