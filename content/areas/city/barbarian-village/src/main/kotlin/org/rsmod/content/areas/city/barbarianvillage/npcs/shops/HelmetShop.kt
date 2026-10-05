package org.rsmod.content.areas.city.barbarianvillage.npcs.shops

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

class HelmetShop @Inject constructor(private val shops: Shops) : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1("npc.peksa") { shopDialogue(it.npc) }
        onOpNpc3("npc.peksa") { player.openHelmetShop(it.npc) }
    }

    private fun Player.openHelmetShop(npc: Npc) {
        shops.open(this, npc, "Helmet Shop", "inv.helmetshop")
    }

    private suspend fun ProtectedAccess.shopDialogue(npc: Npc) =
        startDialogue(npc) { shopKeeper(npc) }

    private suspend fun Dialogue.shopKeeper(npc: Npc) {
        chatNpc(happy, "Are you intrested in buying or selling a helmet?")

        val choice = choice2(
            "I could be, yes.", 1,
            "No, I'll pass on that.", 2,
        )

        when (choice) {
            1 -> player.openHelmetShop(npc)
            2 -> {
                chatPlayer(neutral, "No, I'll pass on that.")
                chatNpc(neutral, "Well, come back if you change your mind.")
            }
        }
    }

}
