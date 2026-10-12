package org.rsmod.content.areas.city.ardougne.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Richard_(Ardougne)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:song_of_the_elves] new taxes dialogue
class FarmingShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(RICHARD) { startDialogue(it.npc) { richard(it.npc) } }
        onOpNpc3(RICHARD) { player.openFarmingShop(it.npc) }
    }

    private fun Player.openFarmingShop(npc: Npc) {
        shops.open(this, npc, "Richard's Farming shop.", "inv.farming_shop_2")
    }

    private suspend fun Dialogue.richard(npc: Npc) {
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
            3 -> chatPlayer(neutral, "I'm okay, thank you.")
        }
    }

    private companion object {
        const val RICHARD = "npc.farming_shopkeeper_3"
    }
}
