package org.rsmod.content.areas.city.varlamore.civitas.npcs.shops

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
class OuterFortisShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(SPIKE) { startDialogue(it.npc) { spike(it.npc) } }
        onOpNpc3(SPIKE) { player.openSpikes(it.npc) }
        onOpNpc1(BARTENDER) { startDialogue(it.npc) { bartender(it.npc) } }
        onOpNpc3(BARTENDER) { player.openPub(it.npc) }
        onOpNpc1(SHOPKEEPER) { startDialogue(it.npc) { shopkeeper(it.npc) } }
        onOpNpc3(SHOPKEEPER) { player.openGeneralStore(it.npc) }
    }

    private fun Player.openSpikes(npc: Npc) {
        shops.open(this, npc, "Spike's Spikes", "inv.fortis_shop_maces")
    }

    private fun Player.openPub(npc: Npc) {
        shops.open(this, npc, "Atlazora's Rest", "inv.pub_fortis_2")
    }

    private fun Player.openGeneralStore(npc: Npc) {
        shops.open(this, npc, "Outer Fortis General Store", "inv.fortis_shop_general_2")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spike
    private suspend fun Dialogue.spike(npc: Npc) {
        chatNpc(happy, "Welcome to Spike's Spikes. Want a mace?")
        while (true) {
            when (choice3("Maybe...", 1, "Spike's Spikes?", 2, "I'm good.", 3)) {
                1 -> {
                    chatPlayer(neutral, "Maybe...")
                    chatNpc(happy, "You want a mace. Have a look.")
                    return player.openSpikes(npc)
                }
                2 -> {
                    chatPlayer(confused, "Spike's Spikes?")
                    chatNpc(
                        happy,
                        "Yeah. I'm Spike and I sell things with spikes, so Spike's Spikes.",
                    )
                    chatPlayer(quiz, "Your name is Spike?")
                    chatNpc(neutral, "No, my name is Memmis, but everyone calls me Spike.")
                    chatPlayer(quiz, "Why do they call you Spike?")
                    chatNpc(
                        neutral,
                        "No idea. Maybe it's because I sell things with spikes. So, want a mace?",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'm good.")
                    chatNpc(neutral, "Wrong answer. You'll be back.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(Atlazora%27s_Rest)
    private suspend fun Dialogue.bartender(npc: Npc) {
        chatNpc(
            happy,
            "Welcome to Atlazora's Rest. If you're in need of a drink, then you're in the right " +
                "place.",
        )
        when (choice2("Show me what you have.", 1, "I'm good, thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Show me what you have.")
                player.openPub(npc)
            }
            2 -> {
                chatPlayer(neutral, "I'm good, thanks.")
                chatNpc(neutral, "Fair enough.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shopkeeper_(Outer_Fortis)
    // TODO(content): [system:shop_stock] inv.fortis_shop_general_2 has no stock definition in
    //  .data/raw-cache/server/shops, so the store opens empty
    private suspend fun Dialogue.shopkeeper(npc: Npc) {
        chatNpc(happy, "Hello there. How can I help you?")
        when (choice2("What do you have for sale?", 1, "I'm good, thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have for sale?")
                chatNpc(happy, "See for yourself.")
                player.openGeneralStore(npc)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Very well")
            }
        }
    }

    private companion object {
        const val SPIKE = "npc.fortis_shop_maces"
        const val BARTENDER = "npc.fortis_bartender_2"
        const val SHOPKEEPER = "npc.fortis_shop_general_2"
    }
}
