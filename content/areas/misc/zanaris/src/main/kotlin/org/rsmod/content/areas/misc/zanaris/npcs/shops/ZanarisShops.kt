package org.rsmod.content.areas.misc.zanaris.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ZanarisShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (keeper in listOf("npc.generalshopkeeper8", "npc.generalassistant8")) {
            onOpNpc1(keeper) { startDialogue(it.npc) { generalStore(it.npc) } }
            onOpNpc3(keeper) { player.openGeneralStore(it.npc) }
        }
        onOpNpc1("npc.jakut") { startDialogue(it.npc) { jukat(it.npc) } }
        onOpNpc3("npc.jakut") { player.openJukat(it.npc) }
        onOpNpc1("npc.irksol") { startDialogue(it.npc) { irksol(it.npc) } }
        onOpNpc3("npc.irksol") { player.openIrksol(it.npc) }
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Zanaris General Store", "inv.generalshop8")
    }

    private fun Player.openJukat(npc: Npc) {
        shops.open(this, npc, "Jukat", "inv.dragonswordshop")
    }

    private fun Player.openIrksol(npc: Npc) {
        shops.open(this, npc, "Irksol", "inv.cheapringshop")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fairy_shop_keeper
    // https://oldschool.runescape.wiki/w/Transcript:Fairy_shop_assistant
    private suspend fun Dialogue.generalStore(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        when (choice2("Yes please. What are you selling?", 1, "No thanks.", 2)) {
            1 -> player.openGeneralStore(npc)
            2 -> chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jukat
    private suspend fun Dialogue.jukat(npc: Npc) {
        chatNpc(happy, "Dragon swords! Here, Dragon swords! Straight from Frenaskrae!")
        when (choice2("Yes please.", 1, "No thanks, I'm just browsing.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes please!")
                player.openJukat(npc)
            }
            2 -> chatPlayer(neutral, "No thanks, I'm just browsing.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Irksol
    private suspend fun Dialogue.irksol(npc: Npc) {
        chatNpc(
            happy,
            "Selling ruby rings! The best deals on rings in over twenty four hundred planes of " +
                "existence!",
        )
        when (choice2("I'm interested in these deals.", 1, "No thanks, just browsing.", 2)) {
            1 -> {
                chatPlayer(happy, "I'm interested in these deals.")
                chatNpc(happy, "Aha! A connoisseur! Check out these beauties!")
                player.openIrksol(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks, just browsing.")
                chatNpc(neutral, "Fair enough.")
            }
        }
    }
}
