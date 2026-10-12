package org.rsmod.content.areas.city.desert.nardah.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc5
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Artimeus
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class NardahHunterShop @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ARTIMEUS) { startDialogue(it.npc) { artimeus(it.npc) } }
        onOpNpc5(ARTIMEUS) { player.openStore(it.npc) }
    }

    private fun Player.openStore(npc: Npc) {
        shops.open(this, npc, "Nardah Hunter Shop.", "inv.hunting_shop_nardah")
    }

    private suspend fun Dialogue.artimeus(npc: Npc) {
        chatNpc(
            happy,
            "Greetings, friend; my business here deals with Hunter related items. Is there " +
                "anything in which I can interest you?",
        )
        val browse =
            choice2(
                "What kinds of items do you stock?",
                true,
                "I'm not in the market for Hunter equipment right now, thanks.",
                false,
            )
        if (browse) {
            chatPlayer(quiz, "What kinds of items do you stock?")
            chatNpc(happy, "Take a look for yourself.")
            player.openStore(npc)
        } else {
            chatPlayer(neutral, "I'm not in the market for Hunter equipment right now, thanks.")
            chatNpc(neutral, "Maybe another time, then.")
        }
    }

    private companion object {
        const val ARTIMEUS = "npc.hunting_shop_owner_nardah"
    }
}
