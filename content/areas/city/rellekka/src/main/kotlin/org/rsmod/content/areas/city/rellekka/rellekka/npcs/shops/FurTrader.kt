package org.rsmod.content.areas.city.rellekka.rellekka.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.areas.city.rellekka.fremennikName
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Fur_trader_(Rellekka)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:thieving] reactions after stealing from his stall
class FurTrader @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_fur_monger") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                startDialogue(it.npc) { fremennik(it.npc) }
            } else {
                startDialogue(it.npc) { chatNpc(angry, "I don't sell to outerlanders.") }
            }
        }
        onOpNpc3("npc.viking_fur_monger") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                player.openShop(it.npc)
            } else {
                mes("Only Fremenniks may purchase furs here.")
            }
        }
    }

    // The cache names inv.viking_furshop "Ardougne Fur Stall."; the wiki's interface title is
    // "Fremennik Fur Trader".
    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Fremennik Fur Trader", "inv.viking_furshop")
    }

    private suspend fun Dialogue.fremennik(npc: Npc) {
        chatNpc(happy, "Welcome back ${fremennikName()}! Have you seen the furs I have today?")
        player.openShop(npc)
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
    }
}
