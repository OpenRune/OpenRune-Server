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

// https://oldschool.runescape.wiki/w/Transcript:Fish_monger
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:thieving] reactions after stealing from his stall
class FishMonger @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_fish_monger") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                startDialogue(it.npc) { fremennik(it.npc) }
            } else {
                startDialogue(it.npc) { outerlander() }
            }
        }
        onOpNpc3("npc.viking_fish_monger") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                player.openShop(it.npc)
            } else {
                mes(ONLY_FREMENNIKS)
            }
        }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Fremennik Fish Monger", "inv.viking_fishmonger")
    }

    private suspend fun Dialogue.fremennik(npc: Npc) {
        chatNpc(happy, "Hello there ${fremennikName()}. Looking for fresh fish?")
        player.openShop(npc)
    }

    private suspend fun Dialogue.outerlander() {
        chatNpc(angry, "I don't sell to outerlanders.")
        access.mes(ONLY_FREMENNIKS)
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
        const val ONLY_FREMENNIKS = "Only Fremenniks may purchase fish here."
    }
}
