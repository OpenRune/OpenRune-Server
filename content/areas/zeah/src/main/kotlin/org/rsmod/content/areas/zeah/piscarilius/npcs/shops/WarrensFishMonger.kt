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

// https://oldschool.runescape.wiki/w/Transcript:Fish_monger_(The_Warrens)
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WarrensFishMonger @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(FISH_MONGER) { startDialogue(it.npc) { fishMonger(it.npc) } }
        onOpNpc3(FISH_MONGER) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Warrens Fish Monger", "inv.warrens_fishmonger")
    }

    private suspend fun Dialogue.fishMonger(npc: Npc) {
        val buy =
            choice2(
                "Could I buy some fish?",
                true,
                "Where do you get your fish from?",
                false,
            )
        if (buy) {
            chatPlayer(quiz, "Could I buy some fish?")
            player.openShop(npc)
            return
        }
        chatPlayer(quiz, "Where do you get your fish from?")
        chatNpc(shifty, "Some questions are best left unanswered.")
        chatPlayer(confused, "Err...")
        chatNpc(neutral, "So would you like to buy some fish?")
        if (choice2("Yes please.", true, "I'll pass.", false)) {
            player.openShop(npc)
        } else {
            chatPlayer(neutral, "I'll pass.")
            chatNpc(neutral, "Suit yourself.")
        }
    }

    private companion object {
        const val FISH_MONGER = "npc.warrens_fishmonger"
    }
}
