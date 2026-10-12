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

// https://oldschool.runescape.wiki/w/Transcript:Robyn
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class ShayzienStyles @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(ROBYN) { startDialogue(it.npc) { robyn(it.npc) } }
        onOpNpc3(ROBYN) { player.openShop(it.npc) }
    }

    private fun Player.openShop(npc: Npc) {
        shops.open(this, npc, "Shayzien Styles", "inv.shayzien_clothesshop")
    }

    private suspend fun Dialogue.robyn(npc: Npc) {
        chatNpc(happy, "Hello there! Is there anything I can help you with?")
        val option =
            choice3(
                "I'd like to buy something.",
                1,
                "How's business going?",
                2,
                "No thanks, I'm just browsing.",
                3,
            )
        when (option) {
            1 -> buy(npc)
            2 -> {
                chatPlayer(quiz, "How's business going?")
                chatNpc(
                    sad,
                    "Its a little slow... There's a lot of soldiers around these parts, so many " +
                        "people just wear a uniform.",
                )
                chatNpc(
                    happy,
                    "Although, I'm just happy to be here! Osten recently took me on as his " +
                        "assistant, you see. I've always wanted to work in fashion!",
                )
                chatNpc(
                    quiz,
                    "Anyway, enough about me... Was there something I could help you with? " +
                        "Perhaps a new cape? ...Or a nice new chef's hat?",
                )
                val buy =
                    choice2(
                        "I'd like to buy something.",
                        true,
                        "No thanks, I'm just browsing.",
                        false,
                    )
                if (buy) buy(npc) else browsing()
            }
            else -> browsing()
        }
    }

    private suspend fun Dialogue.buy(npc: Npc) {
        chatPlayer(happy, "I'd like to buy something.")
        player.openShop(npc)
    }

    private suspend fun Dialogue.browsing() {
        chatPlayer(neutral, "No thanks, I'm just browsing.")
        chatNpc(happy, "I'll be right here if you need anything!")
    }

    private companion object {
        const val ROBYN = "npc.shayzien_clothesshop"
    }
}
