package org.rsmod.content.areas.zeah.shayzien.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Daryl
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class DarylsRangingSurplus @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(DARYL) { startDialogue(it.npc) { daryl(it.npc) } }
        onOpNpc3(DARYL) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Daryl's Ranging Surplus", "inv.shayzien_rangeshop")
    }

    private suspend fun Dialogue.daryl(npc: Npc) {
        chatNpc(neutral, "What are you buying?")
        val option =
            choice3(
                "Show me what you have.",
                1,
                "What do you do around here?",
                2,
                "I don't think I need anything.",
                3,
            )
        when (option) {
            1 -> showMe(npc)
            2 -> {
                chatPlayer(quiz, "What do you do around here?")
                chatNpc(
                    neutral,
                    "I'm responsible for organising and selling off any surplus ranging supplies.",
                )
                chatNpc(
                    neutral,
                    "Those fellows over in Lovakengj are an efficient bunch... They often " +
                        "produce far more than the army actually needs.",
                )
                chatNpc(
                    happy,
                    "I've got a wide selection of ranged weapons and ammo, if you wanted to take " +
                        "a look? Its all good stuff.",
                )
                val show =
                    choice2(
                        "Show me what you have.",
                        true,
                        "I don't think I need anything.",
                        false,
                    )
                if (show) showMe(npc) else noThanks()
            }
            else -> noThanks()
        }
    }

    private suspend fun Dialogue.showMe(npc: Npc) {
        chatPlayer(happy, "Show me what you have.")
        player.openShop(npc)
    }

    private suspend fun Dialogue.noThanks() {
        chatPlayer(neutral, "I don't think I need anything.")
        chatNpc(angry, "Hmph...")
    }

    private companion object {
        const val DARYL = "npc.shayzien_rangeshop"
    }
}
