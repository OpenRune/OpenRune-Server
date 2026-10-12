package org.rsmod.content.areas.zeah.piscarilius.npcs

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class WarrensGapNpcs : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(BARTENDER) { startDialogue(it.npc) { bartender() } }
        onOpNpc1("npc.piscquest_thug") { startDialogue(it.npc) { devanRutter() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(The_Warrens)
    // TODO(content): [verify:in_game] whether the free-space check counts the slot freed by the
    // last coins
    private suspend fun Dialogue.bartender() {
        val option =
            choice2("Could I buy a beer please?", 1, "Have you heard any rumours here?", 2)
        if (option == 2) {
            chatPlayer(quiz, "Have you heard any rumours here?")
            chatNpc(neutral, "No, it hasn't been very busy lately.")
            return
        }
        chatPlayer(quiz, "Could I buy a beer please?")
        chatNpc(happy, "Sure, that will be 2 gold coins please.")
        if (player.inv.count(COINS) < BEER_PRICE) {
            chatPlayer(sad, "I don't have enough coins.")
            return
        }
        chatPlayer(happy, "Here you go.")
        if (player.inv.isFull()) {
            chatNpc(neutral, "You don't have enough room for it.")
            return
        }
        if (!player.invTakeFee(BEER_PRICE)) {
            return
        }
        player.invAdd(player.inv, BEER)
        mesbox("You buy a pint of beer!")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Devan_Rutter
    // TODO(content): [quest:the_queen_of_thieves] the wiki only has the after-quest line
    private suspend fun Dialogue.devanRutter() {
        chatPlayer(quiz, "So can I go in now?")
        chatNpc(angry, "Yes! Now get out of my sight!")
    }

    internal companion object {
        const val BARTENDER = "npc.warrens_barman"
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val BEER_PRICE = 2
    }
}
