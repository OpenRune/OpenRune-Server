package org.rsmod.content.areas.zeah.piscarilius.npcs

import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Bast
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Bast : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kourend_entrance_bartender") { startDialogue(it.npc) { bast() } }
    }

    private suspend fun Dialogue.bast() {
        chatNpc(happy, "What can I do for you, traveller?")
        val option =
            choice4(
                "I'll have a pint.",
                1,
                "What is this place?",
                2,
                "Have you heard any rumours here?",
                3,
                "I'm afraid not.",
                4,
            )
        when (option) {
            1 -> pint()
            2 -> {
                chatPlayer(quiz, "What is this place?")
                chatNpc(
                    happy,
                    "My pub's called The Legless Faun. It's where all the newcomers find " +
                        "themselves after the long voyage from the east. A good drink takes the " +
                        "edge off, y'know? So, fancy a pint?",
                )
                if (choice2("Sure, I'll have a pint.", true, "I'm afraid not.", false)) {
                    pint()
                } else {
                    afraidNot()
                }
            }
            3 -> {
                chatPlayer(quiz, "Have you heard any rumours here?")
                // TODO(content): [quest:client_of_kourend] later rumours once Client of Kourend
                //  has started (see The Golden Field)
                chatNpc(
                    neutral,
                    "Apparently Veos is looking for some help with something. He hangs around " +
                        "the docks of Port Piscarilius.",
                )
            }
            4 -> afraidNot()
        }
    }

    // TODO(content): [verify:in_game] whether paying with the last coins frees a slot for the beer
    private suspend fun Dialogue.pint() {
        chatPlayer(happy, "I'll have a pint.")
        chatNpc(happy, "That'll be 4 gold pieces.")
        if (player.inv.count(COINS) < PRICE) {
            chatPlayer(sad, "I don't have enough coin.")
            chatNpc(neutral, "I can't just be handing out free beers...")
            chatPlayer(shifty, "Couldn't I just take one off those tables?")
            chatNpc(neutral, "...I suppose, if that's the kind of person you are.")
            return
        }
        if (player.inv.isFull()) {
            chatNpc(neutral, "Sorry, you don't seem to have room to hold it.")
            chatPlayer(sad, "Oops!")
            return
        }
        if (!player.invTakeFee(PRICE)) {
            return
        }
        player.invAdd(player.inv, BEER)
        chatPlayer(happy, "Thanks, Bast.")
    }

    private suspend fun Dialogue.afraidNot() {
        chatPlayer(neutral, "I'm afraid not as I've something to attend to.")
        chatNpc(
            laugh,
            "A wise man once told me to never fear a drink... But perhaps I'm a little bias.",
        )
    }

    private companion object {
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val PRICE = 4
    }
}
