package org.rsmod.content.areas.city.alkharid.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.output.mes
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Silk_trader
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:family_crest] "I'm in search of a man named Avan Fitzharmon."
class SilkTrader @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.silk_trader") { startDialogue(it.npc) { silkTrader() } }
    }

    private suspend fun Dialogue.silkTrader() {
        chatNpc(happy, "Do you want to buy any fine silks?")
        when (choice2("How much are they?", 1, "No. Silk doesn't suit me.", 2)) {
            1 -> {
                chatPlayer(quiz, "How much are they?")
                chatNpc(happy, "3 gp.")
                haggle()
            }
            2 -> chatPlayer(neutral, "No. Silk doesn't suit me.")
        }
    }

    private suspend fun Dialogue.haggle() {
        when (choice2("No. That's too much for me.", 1, "Okay, that sounds good.", 2)) {
            1 -> {
                chatPlayer(neutral, "No. That's too much for me.")
                chatNpc(neutral, "2 gp and that's as low as I'll go.")
                chatNpc(
                    neutral,
                    "I'm not selling it for any less. You'll probably go and sell it in Varrock " +
                        "for a profit, anyway.",
                )
                when (choice2("2 gp sounds good.", 1, "No, really. I don't want it.", 2)) {
                    1 -> {
                        chatPlayer(happy, "2 gp sounds good.")
                        buySilk(DISCOUNT_PRICE)
                    }
                    2 -> {
                        chatPlayer(neutral, "No, really. I don't want it.")
                        chatNpc(neutral, "Okay, but that's the best price you're going to get.")
                    }
                }
            }
            2 -> {
                chatPlayer(happy, "Okay, that sounds good.")
                buySilk(FULL_PRICE)
            }
        }
    }

    private suspend fun Dialogue.buySilk(price: Int) {
        if (!player.invTakeFee(price)) {
            chatPlayer(sad, "Oh dear. I don't have enough money.")
            chatNpc(neutral, "Well, come back when you do have some money!")
            return
        }
        player.invAddOrDrop(objRepo, "obj.silk")
        player.mes("You buy some silk for $price gp.")
    }

    private companion object {
        const val FULL_PRICE = 3
        const val DISCOUNT_PRICE = 2
    }
}
