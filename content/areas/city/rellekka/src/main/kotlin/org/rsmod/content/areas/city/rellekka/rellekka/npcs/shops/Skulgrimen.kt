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

// https://oldschool.runescape.wiki/w/Transcript:Skulgrimen
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [skill:crafting] rock-shell armour crafting (missing from the wiki transcript)
class Skulgrimen @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.viking_weapons_salesman") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                startDialogue(it.npc) { fremennik(it.npc) }
            } else {
                startDialogue(it.npc) { outerlander() }
            }
        }
        onOpNpc3("npc.viking_weapons_salesman") {
            if (QuestRequirements.hasCompleted(player, FREMENNIK_TRIALS)) {
                player.openShop(it.npc)
            } else {
                mes("Only Fremenniks may purchase weapons and armour here.")
            }
        }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Skulgrimen's Battle Gear", "inv.viking_weapons_shop")
    }

    private suspend fun Dialogue.fremennik(npc: Npc) {
        chatNpc(happy, "Hello again ${fremennikName()}. Come to see what's for sale?")
        player.openShop(npc)
    }

    private suspend fun Dialogue.outerlander() {
        chatPlayer(happy, "Hello!")
        chatNpc(
            neutral,
            "Sorry. I can't sell weapons to outerlanders. Wouldn't be right. Against our beliefs.",
        )
    }

    private companion object {
        const val FREMENNIK_TRIALS = "quest_fremenniktrials"
    }
}
