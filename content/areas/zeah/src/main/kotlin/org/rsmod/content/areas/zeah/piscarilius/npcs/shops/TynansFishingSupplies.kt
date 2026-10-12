package org.rsmod.content.areas.zeah.piscarilius.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Tynan
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:treasure_trails] easy emote clue reward
class TynansFishingSupplies @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(TYNAN) { startDialogue(it.npc) { tynan(it.npc) } }
        onOpNpc3(TYNAN) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Tynan's Fishing Supplies", "inv.piscarilius_fishing_supplies")
    }

    private suspend fun Dialogue.tynan(npc: Npc) {
        chatNpc(
            happy,
            "If you're looking for fishing supplies, you've come to the right place.",
        )
        val trade =
            choice2(
                "Let's see what you've got for sale.",
                true,
                "I'll be on my way.",
                false,
            )
        if (trade) {
            chatPlayer(happy, "Let's see what you've got for sale.")
            player.openShop(npc)
        } else {
            chatPlayer(neutral, "I'll be on my way.")
            chatNpc(happy, "See you around.")
        }
    }

    private companion object {
        const val TYNAN = "npc.piscarilius_fishing_supplies_trader"
    }
}
