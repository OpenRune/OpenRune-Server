package org.rsmod.content.areas.misc.tzhaar.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:TzHaar-Hur-Tel
// https://oldschool.runescape.wiki/w/Transcript:TzHaar-Hur-Lek
// https://oldschool.runescape.wiki/w/Transcript:TzHaar-Hur-Rin
// https://oldschool.runescape.wiki/w/Transcript:TzHaar-Mej-Roh
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:ironman_shops] Hur-Lek and Hur-Rin have _gim/_uim stock invs
class TzHaarShopkeepers @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (shop in SHOPS) {
            onOpNpc1(shop.npc) { startDialogue(it.npc) { shopkeeper(it.npc, shop) } }
            onOpNpc3(shop.npc) { player.openShop(it.npc, shop) }
        }
    }

    private fun Player.openShop(npc: Npc, shop: Shop) {
        shops.open(this, npc, shop.title, shop.inv)
    }

    private suspend fun Dialogue.shopkeeper(npc: Npc, shop: Shop) {
        chatNpc(quiz, "Can I help you ${player.tzhaarName()}?")
        val option =
            choice3(
                "What do you have to trade?",
                1,
                "What did you call me?",
                2,
                "No I'm fine thanks.",
                3,
            )
        when (option) {
            1 -> player.openShop(npc, shop)
            2 -> whatDidYouCallMe(shop.mej)
            3 -> chatPlayer(neutral, "No I'm fine thanks.")
        }
    }

    private suspend fun Dialogue.whatDidYouCallMe(mej: Boolean) {
        val caste = TzHaarCaste.of(player)
        chatNpc(confused, "Are you not ${player.tzhaarName()}?")
        chatPlayer(confused, "What's a '${caste.title}'?")
        chatNpc(neutral, caste.description)
        chatPlayer(neutral, "Well yes I suppose I am...")
        chatNpc(happy, "Then you ${caste.title}!")
        val option =
            choice4(
                "What are you then?",
                1,
                "Thanks for explaining it.",
                2,
                "I guess so...",
                3,
                "No I'm not!",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you then?")
                if (mej) mejCaste() else hurCaste()
            }
            2 -> chatPlayer(happy, "Thanks for explaining it.")
            3 -> {
                chatPlayer(neutral, "I guess so...")
                chatNpc(neutral, "Well then, no problems.")
            }
            4 -> {
                chatPlayer(angry, "No I'm not!")
                chatNpc(laugh, "What ever you say, crazy JalYt!")
            }
        }
    }

    private suspend fun Dialogue.hurCaste() {
        chatNpc(neutral, "Silly JalYt, I am TzHaar-Hur, one of the crafters for this city.")
        chatNpc(
            neutral,
            "There are the wise TzHaar-Mej who guide us, the mighty TzHaar-Ket who guard us, " +
                "and the swift TzHaar-Xil who hunt for our food.",
        )
    }

    private suspend fun Dialogue.mejCaste() {
        chatNpc(neutral, "Foolish JalYt, I am TzHaar-Mej, one of the mystics of this city.")
        val option = choice2("What other types are there?", 1, "Ah ok then.", 2)
        when (option) {
            1 -> {
                chatPlayer(quiz, "What other types are there?")
                chatNpc(
                    neutral,
                    "There are the mighty TzHaar-Ket who guard us, the swift TzHaar-Xil who hunt " +
                        "for our food, and the skilled TzHaar-Hur who craft our homes and tools.",
                )
            }
            2 -> chatPlayer(neutral, "Ah ok then.")
        }
    }

    private data class Shop(val npc: String, val title: String, val inv: String, val mej: Boolean)

    private companion object {
        val SHOPS =
            listOf(
                Shop(
                    "npc.tzhaar_merchant_equipment",
                    "TzHaar-Hur-Tel's Equipment Store",
                    "inv.tzhaar_shop_equipment",
                    mej = false,
                ),
                Shop(
                    "npc.tzhaar_merchant_oreandgem",
                    "TzHaar-Hur-Lek's Ore and Gem Store",
                    "inv.tzhaar_shop_oreandgem",
                    mej = false,
                ),
                Shop(
                    "npc.tzhaar_merchant_cityoreandgem",
                    "TzHaar-Hur-Rin's Ore and Gem Store",
                    "inv.tzhaar_shop_cityoreandgem",
                    mej = false,
                ),
                Shop(
                    "npc.tzhaar_merchant_rune",
                    "TzHaar-Mej-Roh's Rune Store",
                    "inv.tzhaar_shop_rune",
                    mej = true,
                ),
            )
    }
}
