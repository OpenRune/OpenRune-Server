package org.rsmod.content.areas.city.karamja.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Zembo @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ZEMBO) { startDialogue(it.npc) { zembo() } }
        onOpNpc3(ZEMBO) { player.openBar() }
    }

    private fun Player.openBar() =
        shops.open(
            player = this,
            title = TITLE,
            shopInv = SHOP_INV,
            buyPercentage = BUY_PERCENTAGE,
            sellPercentage = SELL_PERCENTAGE,
            changePercentage = CHANGE_PERCENTAGE,
        )

    private suspend fun Dialogue.zembo() {
        chatNpc(
            happy,
            "Hey, are you wanting to try some of my fine wines and spirits? All brewed " +
                "locally on Karamja island.",
        )
        if (choice2("Yes please.", true, "No, thank you.", false)) {
            access.player.openBar()
        } else {
            chatPlayer(neutral, "No, thank you.")
        }
    }

    private companion object {
        const val ZEMBO = "npc.zembo"
        const val TITLE = "Karamja Wines, Spirits, and Beers."
        const val SHOP_INV = "inv.boozeshop"
        const val SELL_PERCENTAGE = 100.0
        const val BUY_PERCENTAGE = 70.0
        const val CHANGE_PERCENTAGE = 2.0
    }
}
