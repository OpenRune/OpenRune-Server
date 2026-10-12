package org.rsmod.content.areas.city.karamja.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Yanni_Salika
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:shilo_village] buying antiques used on Yanni
// TODO(content): [quest:one_small_favour] quest dialogue and the black prism
class Yanni : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.shiloantiques") { startDialogue(it.npc) { yanni() } }
    }

    private suspend fun Dialogue.yanni() {
        chatPlayer(happy, "Hello there!")
        chatNpc(happy, "Greetings Bwana! My name is Yanni and I buy and sell antiques.")
        val business =
            choice2(
                "Tell me about this antiques business.",
                true,
                "Do you have any interesting items for sale?",
                false,
            )
        if (business) antiquesBusiness() else itemsForSale()
    }

    private suspend fun Dialogue.antiquesBusiness() {
        chatPlayer(quiz, "Tell me about this antiques business.")
        chatNpc(
            neutral,
            "I buy antiques and other interesting items. If you have any interesting items that " +
                "you might want to sell me, please let me see them and I'll offer you a fair " +
                "price.",
        )
        chatNpc(neutral, "Would you like me to have a look at your items and give you a quote?")
        if (!choice2("Yes please!", true, "Maybe some other time?", false)) {
            chatPlayer(neutral, "Maybe some other time?")
            chatNpc(happy, "Sure thing. Have a nice day Bwana.")
            return
        }
        chatPlayer(happy, "Yes please!")
        val quotes = ANTIQUES.filter { (objs, _) -> objs.any { it in player.inv } }
        if (quotes.isEmpty()) {
            chatNpc(neutral, "Sorry Bwana, you have nothing I am interested in.")
            return
        }
        for ((_, quote) in quotes) {
            chatNpc(happy, quote)
        }
        if (quotes.size == 1) {
            chatNpc(
                happy,
                "And that's the only item I am interested in. If you want to sell me that item, " +
                    "simply show it to me.",
            )
        } else {
            chatNpc(
                happy,
                "Those are the items I am interested in Bwana. If you want to sell me those " +
                    "items, simply show them to me.",
            )
        }
    }

    // TODO(content): [verify:in_game] the reply without the Beads of the Dead is missing
    // TODO(content): [verify:in_game] the beads in the bank also count as having them
    private suspend fun Dialogue.itemsForSale() {
        chatPlayer(quiz, "Do you have any interesting items for sale?")
        if (BEADS_OF_THE_DEAD in player.inv) {
            val title = if (access.isBodyTypeA()) "Sir" else "Madam"
            chatNpc(neutral, "Sorry, $title, I have nothing that you need.")
        }
    }

    private companion object {
        const val BEADS_OF_THE_DEAD = "obj.zqdeadbeads"
        val ANTIQUES =
            listOf(
                listOf("obj.zqbonekey") to "I'll give you 100 coins for the Bone Key...",
                listOf("obj.zqplaque") to "I'll give you 100 coins for the Stone Plaque...",
                listOf("obj.zqberviriusscroll") to
                    "I'll give you 100 coins for the tattered scroll...",
                listOf("obj.zqrashiliyiascroll") to
                    "I'll give you 100 coins for the crumpled scroll...",
                listOf(
                    "obj.zqcrystal",
                    "obj.zqcrystal_blue",
                    "obj.zqcrystal_red",
                    "obj.zqcrystal_yellow",
                    "obj.zqcrystal_white",
                ) to "I'll give you 500 coins for your locating crystal...",
                listOf(BEADS_OF_THE_DEAD) to
                    "I'll give you 1000 coins for your 'Beads of the Dead'...",
            )
    }
}
