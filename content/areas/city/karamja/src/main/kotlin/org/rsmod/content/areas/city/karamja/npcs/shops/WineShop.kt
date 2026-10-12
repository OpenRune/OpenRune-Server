package org.rsmod.content.areas.city.karamja.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Zembo
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [miniquest:alfred_grimhands_barcrawl] Ape Bite Liqueur for 7 coins
class WineShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.zembo") { startDialogue(it.npc) { zembo(it.npc) } }
        onOpNpc3("npc.zembo") { player.openWineShop(it.npc) }
    }

    private fun Player.openWineShop(npc: Npc) {
        shops.open(this, npc, "Karamja Wines, Spirits, and Beers.", "inv.boozeshop")
    }

    private suspend fun Dialogue.zembo(npc: Npc) {
        chatNpc(
            happy,
            "Hey, are you wanting to try some of my fine wines and spirits? All brewed locally " +
                "on Karamja island.",
        )
        if (choice2("Yes please.", true, "No, thank you.", false)) {
            player.openWineShop(npc)
        } else {
            chatPlayer(neutral, "No, thank you.")
        }
    }
}
