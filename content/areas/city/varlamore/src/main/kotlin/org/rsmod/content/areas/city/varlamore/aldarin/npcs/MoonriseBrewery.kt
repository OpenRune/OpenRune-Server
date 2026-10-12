package org.rsmod.content.areas.city.varlamore.aldarin.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class MoonriseBrewery : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(LUCREZIA) { startDialogue(it.npc) { lucrezia() } }
        onOpNpc1(FOREMAN) { startDialogue(it.npc) { vineyardForeman() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Lucrezia
    private suspend fun Dialogue.lucrezia() {
        chatPlayer(quiz, "Hello, I wonder if you could help me on this whole brewing thing...")
        chatNpc(happy, "I might be able to - what do you need to know?")
        while (true) {
            val option =
                choice5(
                    "How do I brew ales?",
                    1,
                    "How do I brew cider?",
                    2,
                    "What do I do once my ale has matured?",
                    3,
                    "Do you have any spare ale yeast?",
                    4,
                    "That's all I need to know, thanks.",
                    5,
                    title = "Select an option",
                )
            when (option) {
                1 -> brewAles()
                2 -> brewCider()
                3 -> {
                    chatPlayer(quiz, "What do I do once my ale has matured?")
                    chatNpc(
                        neutral,
                        "Well, once you've got a full vat of the good stuff, just turn the valve " +
                            "and your barrel will fill up with eight pints of whatever your " +
                            "chosen tipple is. Mind it's an empty barrel, though.",
                    )
                }
                4 -> aleYeast()
                else -> {
                    chatPlayer(happy, "That's all I need to know, thanks.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.brewAles() {
        chatPlayer(quiz, "How do I brew ales?")
        chatNpc(
            neutral,
            "Well first off you need to fill the vat with water - two bucketfuls should do the " +
                "trick. Then you'll need to put in two handfuls of barley malt - that's roasted " +
                "barley by the way.",
        )
        chatNpc(
            neutral,
            "After that you'll be putting your main ingredient in - this will decide which ale " +
                "it is you're brewing. There should be some good guides around with recipes in.",
        )
        chatNpc(
            neutral,
            "Lastly you pour a pot full of ale yeast in, which'll start it off fermenting. Then " +
                "all you have to do is wait for the good stuff.",
        )
    }

    private suspend fun Dialogue.brewCider() {
        chatPlayer(quiz, "How do I brew cider?")
        chatNpc(
            neutral,
            "First you'll need some apples. You'll need to crush them using this cider-press - " +
                "four apples should make a full bucket of apple-mush. Take four buckets of mush " +
                "and fill up the vat.",
        )
        chatNpc(
            neutral,
            "After that you pour a pot full of ale yeast in, which'll start it off fermenting. " +
                "Then all you have to do is wait for the good stuff.",
        )
    }

    private suspend fun Dialogue.aleYeast() {
        chatPlayer(quiz, "Do you have any spare ale yeast?")
        chatNpc(
            neutral,
            "Well, as a matter of fact I do, although I wouldn't describe it as spare. This ale " +
                "yeast I've got is the best money can buy, but if you've got a pot I'll fill it " +
                "for you for 25GP - very cheap as it happens.",
        )
        val hasPot = POT in player.inv
        val hasCoins = player.inv.count(COINS) >= YEAST_PRICE
        when {
            hasPot && !hasCoins -> chatPlayer(sad, "I don't have enough money I'm afraid.")
            !hasPot && hasCoins -> chatPlayer(sad, "I don't have an empty pot I'm afraid.")
            !hasPot -> chatPlayer(sad, "I don't have an empty pot, or enough money I'm afraid.")
            else -> {
                val option =
                    choice2(
                        "That's a good deal - please fill my pot with ale yeast for 25GP.",
                        1,
                        "No, that's too much I'm afraid.",
                        2,
                        title = "Select an option",
                    )
                if (option == 1) {
                    chatPlayer(
                        happy,
                        "That's a good deal - please fill my pot with ale yeast for 25GP.",
                    )
                    buyYeast()
                } else {
                    chatPlayer(neutral, "No, that's too much I'm afraid.")
                }
            }
        }
    }

    private fun Dialogue.buyYeast() {
        val inventory = player.inv
        player.invTransaction(inventory) {
            val inv = select(inventory)
            delete {
                from = inv
                obj = POT.asRSCM(RSCMType.OBJ)
                strictCount = 1
            }
            delete {
                from = inv
                obj = COINS.asRSCM(RSCMType.OBJ)
                strictCount = YEAST_PRICE
            }
            insert {
                into = inv
                obj = ALE_YEAST.asRSCM(RSCMType.OBJ)
                strictCount = 1
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vineyard_foreman
    // TODO(content): [system:grape_barrel] picking ripe grapes and handing in a full barrel
    // TODO(content): [system:met_flags] "Hopefully you know what you're doing now." when the
    //  foreman has handed out a barrel before
    // TODO(content): [system:banana_plantation] the extra player line for players who have
    //  worked at Luthas' banana plantation
    private suspend fun Dialogue.vineyardForeman() {
        chatNpc(happy, "Nilsal, friend. How goes it?")
        while (true) {
            val option =
                choice3(
                    "Do you need any help in the vineyards?",
                    1,
                    "Doesn't it get hot standing out here all day?",
                    2,
                    "I'm good. I'll be going now.",
                    3,
                )
            when (option) {
                1 -> return grapeBarrel()
                2 -> {
                    chatPlayer(quiz, "Doesn't it get hot standing out here all day?")
                    chatNpc(
                        laugh,
                        "Ah, you get used to it. Besides, I have a near-infinite supply of " +
                            "lukewarm grape juice to keep me hydrated!",
                    )
                }
                else -> {
                    chatPlayer(neutral, "I'm good. I'll be going now.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.grapeBarrel() {
        chatPlayer(quiz, "Do you need any help in the vineyards?")
        if (GRAPE_BARREL in player.inv) {
            chatNpc(
                neutral,
                "You've already got that grape barrel I gave you. Fill it with four sets of ripe " +
                    "grapes and bring it back to me.",
            )
            return grapesReminder()
        }
        if (!giveGrapeBarrel()) {
            chatNpc(
                neutral,
                "We could always use a spare pair of hands! I have a grape barrel here for you, " +
                    "but you don't have enough room for it at the moment. Come back once you can " +
                    "carry it.",
            )
            return
        }
        chatNpc(happy, "Well we could always use a spare pair of hands!")
        objbox(GRAPE_BARREL, "The foreman gives you a grape barrel.")
        chatNpc(
            neutral,
            "Here, fill this barrel with four sets of ripe grapes and bring it back to me.",
        )
        grapesReminder()
    }

    private suspend fun Dialogue.grapesReminder() {
        chatPlayer(happy, "Got it! One barrel full of grapes coming right up.")
        chatNpc(
            neutral,
            "Just remember, I need them to be ripe. Generally, perfectly ripe grapes are full " +
                "and vibrant. They'll shine in the sunlight more than others.",
        )
    }

    private fun Dialogue.giveGrapeBarrel(): Boolean {
        val inventory = player.inv
        val result =
            player.invTransaction(inventory) {
                val inv = select(inventory)
                insert {
                    into = inv
                    obj = GRAPE_BARREL.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
    }

    private companion object {
        const val LUCREZIA = "npc.aldarin_brewing_helper"
        const val FOREMAN = "npc.aldarin_vineyard_foreman"
        const val POT = "obj.pot_empty"
        const val COINS = "obj.coins"
        const val ALE_YEAST = "obj.ale_yeast"
        const val GRAPE_BARREL = "obj.aldarin_grape_collection_barrel"
        const val YEAST_PRICE = 25
    }
}
