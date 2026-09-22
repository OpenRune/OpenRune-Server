package org.rsmod.content.areas.city.lumbridge.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

private var Player.heardLanterns by intVarBit("varbit.swamp_candle_guy_conversation")

class SwampCandleSeller : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.swamp_candle_guy") { startDialogue(it.npc) { candleSeller() } }
    }

    private suspend fun Dialogue.candleSeller() {
        chatNpc(neutral, "Do you want a lit candle for 1000 gold?")
        val choice =
            menu(
                buildList {
                    add("Yes please." to 1)
                    add("One thousand gold?!" to 2)
                    add("No thanks, I'd rather curse the darkness." to 3)
                    if (player.heardLanterns == 1) add("How do you make lanterns?" to 4)
                }
            )
        when (choice) {
            1 -> {
                chatPlayer(neutral, "Yes please.")
                buyCandle()
            }
            2 -> tooExpensive()
            3 -> chatPlayer(neutral, "No thanks, I'd rather curse the darkness.")
            else -> lanterns()
        }
    }

    private suspend fun Dialogue.tooExpensive() {
        chatPlayer(shocked, "One thousand gold?!")
        chatNpc(
            neutral,
            "Look, you're not going to be able to survive down that hole without a light source.",
        )
        chatNpc(
            neutral,
            "So you could go off to the candle shop to buy one more cheaply.  You could even make " +
                "your own lantern, which is a lot better.",
        )
        chatNpc(
            neutral,
            "But I bet you want to find out what's down there right now, don't you?  And you can pay " +
                "me 1000 gold for the privilege!",
        )
        player.heardLanterns = 1
        when (
            menu(
                "All right, you win, I'll buy a candle." to 1,
                "No way." to 2,
                "How do you make lanterns?" to 3,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "All right, you win, I'll buy a candle.")
                buyCandle()
            }
            2 -> chatPlayer(angry, "No way.")
            else -> lanterns()
        }
    }

    private suspend fun Dialogue.lanterns() {
        chatPlayer(quiz, "How do you make lanterns?")
        chatNpc(
            neutral,
            "Out of glass.  The more advanced lanterns have a metal component as well.",
        )
        chatNpc(
            neutral,
            "Firstly you can make a simple candle lantern out of glass.  It's just like a candle, but " +
                "the flame isn't exposed, so it's safer.",
        )
        chatNpc(
            neutral,
            "Then you can make an oil lamp, which is brighter but has an exposed flame.  But if you " +
                "make an iron frame for it you can turn it into an oil lantern.",
        )
        chatNpc(
            neutral,
            "Finally there's the bullseye lantern.  You'll need to make a frame out of steel and add " +
                "a glass lens.",
        )
        chatNpc(
            neutral,
            "Once you've made your lamp or lantern, you'll need to make lamp oil for it.  The chemist " +
                "near Rimmington has a machine for that.",
        )
        chatNpc(
            neutral,
            "For any light source, you'll need a tinderbox to light it. Keep your tinderbox handy in " +
                "case it goes out!",
        )
        chatNpc(
            neutral,
            "But if all that's too complicated, you can buy a candle right here for 1000 gold!",
        )
        val buy =
            menu(
                "All right, you win, I'll buy a candle." to true,
                "No thanks, I'd rather curse the darkness." to false,
            )
        if (buy) {
            chatPlayer(neutral, "All right, you win, I'll buy a candle.")
            buyCandle()
        } else {
            chatPlayer(neutral, "No thanks, I'd rather curse the darkness.")
        }
    }

    private suspend fun Dialogue.buyCandle() {
        if (access.inv.count(COINS) < PRICE) {
            chatPlayer(sad, "But I don't have that kind of money on me.")
            chatNpc(neutral, "Well then, no candle for you!")
            return
        }
        access.invDel(access.inv, COINS, PRICE)
        access.invAdd(access.inv, CANDLE)
        chatNpc(neutral, "Here you go then.")
        chatNpc(
            neutral,
            "I should warn you, though, it can be dangerous to take a naked flame down there. You'd " +
                "be better off making a lantern.",
        )
        when (
            menu(
                "What's so dangerous about a naked flame?" to 1,
                "How do you make lanterns?" to 2,
                "Thanks, bye." to 3,
            )
        ) {
            1 -> {
                chatPlayer(quiz, "What's so dangerous about a naked flame?")
                chatNpc(laugh, "Heh heh... You'll find out.")
            }
            2 -> lanterns()
            else -> chatPlayer(neutral, "Thanks, bye.")
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val CANDLE = "obj.lit_candle"
        const val PRICE = 1000
    }
}
