package org.rsmod.content.areas.city.taverley.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Gaius
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] easy clue reward
class TwoHandedShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gaius") { startDialogue(it.npc) { gaius(it.npc) } }
        onOpNpc3("npc.gaius") { player.openTwoHandedShop(it.npc) }
    }

    private fun Player.openTwoHandedShop(npc: Npc) {
        shops.open(this, npc, "Gaius' Two Handed Shop.", "inv.2handedshop")
    }

    private suspend fun Dialogue.gaius(npc: Npc) {
        chatNpc(happy, "Welcome to my two-handed sword shop.")
        when (choice2("Let's trade.", 1, "Thanks, but not today.", 2)) {
            1 -> player.openTwoHandedShop(npc)
            2 -> {
                chatPlayer(neutral, "Thanks, but not today.")
                chatNpc(neutral, "Very well, but do please call again.")
            }
        }
    }
}
