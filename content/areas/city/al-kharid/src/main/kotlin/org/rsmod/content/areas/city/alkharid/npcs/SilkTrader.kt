package org.rsmod.content.areas.city.alkharid.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Sells silk by haggling rather than through a shop; he will come down to 2gp if pushed. */
class SilkTrader : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.silk_trader") { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { haggle() }
    }

    private suspend fun Dialogue.haggle() {
        chatNpc(neutral, "Do you want to buy any fine silks?")
        if (!choice2("How much are they?", true, "No. Silk doesn't suit me.", false)) {
            chatPlayer(neutral, "No. Silk doesn't suit me.")
            return
        }
        chatPlayer(quiz, "How much are they?")
        chatNpc(neutral, "$FULL_PRICE gp.")
        if (choice2("No. That's too much for me.", false, "Okay, that sounds good.", true)) {
            chatPlayer(neutral, "Okay, that sounds good.")
            buySilk(FULL_PRICE)
            return
        }
        chatPlayer(neutral, "No. That's too much for me.")
        chatNpc(neutral, "$HAGGLED_PRICE gp and that's as low as I'll go.")
        chatNpc(
            neutral,
            "I'm not selling it for any less. You'll probably go and sell it in Varrock for a profit, anyway.",
        )
        if (choice2("$HAGGLED_PRICE gp sounds good.", true, "No, really. I don't want it.", false)) {
            chatPlayer(neutral, "$HAGGLED_PRICE gp sounds good.")
            buySilk(HAGGLED_PRICE)
            return
        }
        chatPlayer(neutral, "No, really. I don't want it.")
        chatNpc(neutral, "Okay, but that's the best price you're going to get.")
    }

    private suspend fun Dialogue.buySilk(price: Int) {
        if (access.invTotal(access.inv, COINS) < price) {
            chatPlayer(sad, "Oh dear. I don't have enough money.")
            chatNpc(angry, "Well, come back when you do have some money!")
            return
        }
        if (!access.buyForCoins(SILK, price)) {
            access.mes("You don't have enough inventory space.")
            return
        }
        objbox(SILK, "You buy some silk for $price gp.")
    }

    private companion object {
        private const val COINS = "obj.coins"
        private const val SILK = "obj.silk"
        private const val FULL_PRICE = 3
        private const val HAGGLED_PRICE = 2
    }
}
