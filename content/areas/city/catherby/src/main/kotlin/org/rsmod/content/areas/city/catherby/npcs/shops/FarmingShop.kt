package org.rsmod.content.areas.city.catherby.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Vanessa
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FarmingShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.farming_shopkeeper_2") { startDialogue(it.npc) { vanessa(it.npc) } }
        onOpNpc3("npc.farming_shopkeeper_2") { player.openFarmingShop(it.npc) }
    }

    private fun Player.openFarmingShop(npc: Npc) {
        shops.open(this, npc, "Vanessa's Farming shop.", "inv.farming_shop_4")
    }

    private suspend fun Dialogue.vanessa(npc: Npc) {
        chatNpc(happy, "Hello. How can I help you?")
        val option =
            choice3(
                "What are you selling?",
                1,
                "Can you give me any Farming advice?",
                2,
                "I'm okay, thank you.",
                3,
            )
        when (option) {
            1 -> player.openFarmingShop(npc)
            2 -> {
                chatPlayer(quiz, "Can you give me any Farming advice?")
                chatNpc(neutral, "Yes - ask a gardener.")
            }
            // TODO(content): [verify:in_game] the transcript gives this line to Vanessa
            3 -> chatPlayer(neutral, "I'm okay, thank you.")
        }
    }
}
