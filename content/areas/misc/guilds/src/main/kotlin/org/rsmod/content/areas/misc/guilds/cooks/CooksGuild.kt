package org.rsmod.content.areas.misc.guilds.cooks

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
class CooksGuild @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.merchant_pies") { startDialogue(it.npc) { romily(it.npc) } }
        onOpNpc3("npc.merchant_pies") { player.openPieShop(it.npc) }
    }

    private fun Player.openPieShop(npc: Npc) {
        shops.open(this, npc, "Pie Shop", "inv.shop_pies")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Romily_Weaklax
    // TODO(content): [system:pie_shop_errands] "Sure, what do you need?" assigns a pie-baking
    //  errand, and "I've got those pies you wanted." hands the pies in
    private suspend fun Dialogue.romily(npc: Npc) {
        chatNpc(happy, "Hello and welcome to my pie shop, how can I help you?")
        val option =
            choice3(
                "I'd like to buy some pies.",
                1,
                "Do you need any help?",
                2,
                "I'm good thanks.",
                3,
            )
        when (option) {
            1 -> player.openPieShop(npc)
            2 -> {
                chatPlayer(quiz, "Do you need any help?")
                chatNpc(
                    neutral,
                    "Actually I could, you see I'm running out of stock and I don't have time to " +
                        "bake any more pies. Would you be willing to bake me some pies? I'll pay " +
                        "you well for them.",
                )
            }
            3 -> chatPlayer(neutral, "I'm good thanks.")
        }
    }
}
