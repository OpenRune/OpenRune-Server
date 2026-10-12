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

// https://oldschool.runescape.wiki/w/Transcript:Candle_maker
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:merlins_crystal] black candle dialogue
// TODO(content): [system:treasure_trails] elite clue reward
class CandleShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.candle_maker") { startDialogue(it.npc) { candleMaker(it.npc) } }
        onOpNpc3("npc.candle_maker") { player.openCandleShop(it.npc) }
    }

    private fun Player.openCandleShop(npc: Npc) {
        shops.open(this, npc, "Candle Shop", "inv.candleshop")
    }

    private suspend fun Dialogue.candleMaker(npc: Npc) {
        chatNpc(happy, "Hi! Would you be interested in some of my fine candles?")
        if (choice2("Yes Please.", true, "No thank you.", false)) {
            chatPlayer(happy, "Yes please.")
            player.openCandleShop(npc)
        } else {
            chatPlayer(neutral, "No thank you.")
        }
    }
}
