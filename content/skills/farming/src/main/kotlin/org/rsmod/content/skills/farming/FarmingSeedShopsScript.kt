package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingSeedShopsScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kebos_seed_merchant") { startDialogue(it.npc) { amelia() } }
        onOpNpc3("npc.kebos_seed_merchant") { openSeedShop() }
    }

    private suspend fun Dialogue.amelia() {
        chatNpc(neutral, "Greetings and welcome to my seed shop. Do you want to buy some seeds?")
        if (choice2("Yes, please.", true, "No, thank you.", false)) {
            access.openSeedShop()
        } else {
            chatPlayer(neutral, "No, thank you.")
        }
    }

    private fun ProtectedAccess.openSeedShop() {
        shops.open(
            player = player,
            title = "Amelia's Seed Shop",
            shopInv = "inv.kebos_farming_seed_shop",
            buyPercentage = 60.0,
            sellPercentage = 100.0,
            changePercentage = 2.0,
        )
    }
}
