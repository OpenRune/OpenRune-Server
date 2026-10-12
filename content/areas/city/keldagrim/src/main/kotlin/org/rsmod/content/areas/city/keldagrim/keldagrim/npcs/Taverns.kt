package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class Taverns @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.dwarf_city_barman_rich") { startDialogue(it.npc) { kingsAxeBarman() } }
        onOpNpc1("npc.dwarf_city_inn_keeper_rich") { startDialogue(it.npc) { innKeeper() } }
        onOpNpc1("npc.dwarf_city_barmaid_poor") { startDialogue(it.npc) { barmaid() } }
        onOpNpc1("npc.blandebir") { startDialogue(it.npc) { blandebir() } }
        onOpNpc1("npc.dwarf_city_dwarf_man6") { startDialogue(it.npc) { gauss() } }
        onOpNpc1("npc.dwarf_city_dwarf_man8") { startDialogue(it.npc) { kjut() } }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barman_(King%27s_Axe_Inn)
    private suspend fun Dialogue.kingsAxeBarman() {
        if (player.inv.count(COINS) > 2000) {
            chatNpc(happy, "And what will it be for you, fine friend?")
        } else {
            chatNpc(neutral, "Yes? Can I help you with something?")
        }
        val option =
            choice4(
                "A beer please.",
                1,
                "A dwarven stout please.",
                2,
                "A fine wine please.",
                3,
                "Nothing, thanks.",
                4,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "A beer please.")
                chatNpc(neutral, "That'll be 1 gold coin please.")
                kingsAxeOrder(1, BEER, "Here you go. One delicious pint of beer.")
            }
            2 -> {
                chatPlayer(happy, "A dwarven stout please.")
                chatNpc(happy, "It'll be yours for just 2 gold coins!")
                kingsAxeOrder(
                    2,
                    "obj.dwarven_stout",
                    "One dwarven stout, best in all of Keldagrim!",
                )
            }
            3 -> {
                chatPlayer(happy, "A fine wine please.")
                chatNpc(
                    happy,
                    "Ah, I see you have taste! A jug of wine will cost you 10 gold coins.",
                )
                kingsAxeOrder(10, "obj.jug_wine", "I hope you enjoy it!")
            }
            else -> chatPlayer(neutral, "Nothing, thanks.")
        }
    }

    private suspend fun Dialogue.kingsAxeOrder(price: Int, drink: String, served: String) {
        if (!choice2("Pay.", true, "Don't pay.", false)) {
            chatPlayer(neutral, "Sorry, I changed my mind.")
            return
        }
        if (buy(price, drink)) {
            chatNpc(happy, served)
        } else {
            chatPlayer(sad, "I'm sorry, I seem to have run out of money.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Inn_Keeper
    private suspend fun Dialogue.innKeeper() {
        chatNpc(happy, "Welcome to the King's Axe inn! What can I help you with?")
        chatPlayer(quiz, "Can I have some beer please?")
        chatNpc(neutral, "Go to the bar downstairs. I only deal with residents.")
        chatPlayer(confused, "Residents? People live here?")
        chatNpc(neutral, "No, just guests that stay the night.")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Barmaid
    // TODO(content): [quest:forgettable_tale] "I'd like a Kelda stout please." while in progress
    private suspend fun Dialogue.barmaid() {
        chatNpc(happy, "Welcome to the Laughing Miner pub, human traveller.")
        val option =
            choice4(
                "Who is that man walking around outside?",
                1,
                "I'd like a beer please.",
                2,
                "I'd like some food please.",
                3,
                "I have to go.",
                4,
            )
        when (option) {
            1 -> barmaidSignDwarf()
            2 -> {
                chatPlayer(happy, "I'd like a beer please.")
                chatNpc(neutral, "That'll be 2 gold coins.")
                laughingMinerOrder(2, BEER)
            }
            3 -> {
                chatPlayer(happy, "I'd like some food please.")
                chatNpc(neutral, "I can make you a stew for 20 gold coins.")
                laughingMinerOrder(20, "obj.stew")
            }
            else -> chatPlayer(neutral, "I have to go.")
        }
    }

    private suspend fun Dialogue.barmaidSignDwarf() {
        chatPlayer(quiz, "Who is that man walking around outside?")
        chatNpc(confused, "What man?")
        chatPlayer(neutral, "I mean the dwarf, the one with the sign.")
        chatNpc(
            neutral,
            "Oh, him. Yes, we employ him to advertise our pub, he's the cheapest labour we " +
                "could find. We don't have a lot of money to spare you know, we pay him in beer.",
        )
        chatPlayer(quiz, "But what's wrong with him?")
        chatNpc(neutral, "Well, he's drunk isn't he?")
        chatNpc(
            neutral,
            "I told you, he's cheap. He couldn't get any other work since the Red Axe fired " +
                "him. Been drinking ever since.",
        )
        chatPlayer(quiz, "How did that happen?")
        chatNpc(
            neutral,
            "I'm not quite sure, I think he kept wearing the wrong coloured cap all the time. " +
                "They don't much like it if you don't wear your red uniform into work.",
        )
        chatPlayer(shocked, "They fired him for that??")
        chatNpc(neutral, "The Red Axe will fire you for just about anything if they want to.")
    }

    private suspend fun Dialogue.laughingMinerOrder(price: Int, item: String) {
        if (!choice2("Pay.", true, "Don't pay.", false)) {
            chatPlayer(neutral, "Sorry, I changed my mind.")
            return
        }
        if (buy(price, item)) {
            chatNpc(happy, "Thanks for your custom.")
        } else {
            chatPlayer(sad, "Sorry, I don't have $price coins on me.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blandebir
    private suspend fun Dialogue.blandebir() {
        chatPlayer(
            quiz,
            "Hello, I wonder if you could help me on this whole brewing thing...",
        )
        chatNpc(neutral, "I might be able to - what do you need to know?")
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
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "How do I brew ales?")
                    chatNpc(
                        neutral,
                        "Well first off you need to fill the vat with water - two bucketfuls " +
                            "should do the trick. Then you'll need to put in two handfuls of " +
                            "barley malt - that's roasted barley by the way.",
                    )
                    chatNpc(
                        neutral,
                        "After that you'll be putting your main ingredient in - this will " +
                            "decide which ale it is you're brewing. There should be some good " +
                            "guides around with recipes in.",
                    )
                    chatNpc(
                        neutral,
                        "Lastly you pour a pot full of ale yeast in, which'll start it off " +
                            "fermenting. Then all you have to do is wait for the good stuff.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "How do I brew cider?")
                    chatNpc(
                        neutral,
                        "First you'll need some apples. You'll need to crush them using this " +
                            "cider-press - four apples should make a full bucket of " +
                            "apple-mush. Take four buckets of mush and fill up the vat.",
                    )
                    chatNpc(
                        neutral,
                        "After that you pour a pot full of ale yeast in, which'll start it off " +
                            "fermenting. Then all you have to do is wait for the good stuff.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "What do I do once my ale has matured?")
                    chatNpc(
                        neutral,
                        "Well, once you've got a full vat of the good stuff, just turn the " +
                            "valve and your barrel will fill up with eight pints of whatever " +
                            "your chosen tipple is. Mind it's an empty barrel, though.",
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

    private suspend fun Dialogue.aleYeast() {
        chatPlayer(quiz, "Do you have any spare ale yeast?")
        chatNpc(
            neutral,
            "Well, as a matter of fact I do, although I wouldn't describe it as spare. This ale " +
                "yeast I've got is the best money can buy, but if you've got a pot I'll fill it " +
                "for you for 25GP - very cheap as it happens.",
        )
        val hasPot = player.inv.count(POT) > 0
        val hasCoins = player.inv.count(COINS) >= YEAST_PRICE
        when {
            !hasPot && !hasCoins ->
                chatPlayer(sad, "I don't have an empty pot, or enough money I'm afraid.")
            !hasPot -> chatPlayer(sad, "I don't have an empty pot I'm afraid.")
            !hasCoins -> chatPlayer(sad, "I don't have enough money I'm afraid.")
            else -> {
                val accepted =
                    choice2(
                        "That's a good deal - please fill my pot with ale yeast for 25GP.",
                        true,
                        "No, that's too much I'm afraid.",
                        false,
                    )
                if (!accepted) {
                    chatPlayer(neutral, "No, that's too much I'm afraid.")
                    return
                }
                chatPlayer(
                    happy,
                    "That's a good deal - please fill my pot with ale yeast for 25GP.",
                )
                if (access.invDel(player.inv, COINS, YEAST_PRICE, POT, 1).success) {
                    access.invAdd(player.inv, "obj.ale_yeast")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gauss
    // TODO(content): [verify:in_game] drink used when carrying several; cheers animation
    private suspend fun Dialogue.gauss() {
        chatNpc(happy, "I say, I say, a toast to this fine human!")
        chatPlayer(confused, "What? I mean, err... why?")
        chatNpc(laugh, "Why not!")
        val drink = TOAST_DRINKS.firstOrNull { player.inv.count(it) > 0 }
        when {
            drink != null -> {
                chatPlayer(happy, "Why not indeed! Cheers!")
                chatNpc(happy, "Cheers!")
                access.invReplace(player.inv, drink, 1, "obj.beer_glass")
            }
            player.inv.count("obj.jug_wine") > 0 -> {
                chatPlayer(neutral, "Well, I have this jug of wine...")
                chatNpc(
                    neutral,
                    "No no, get a pint of beer, jugs of wine aren't good to toast with! Wine " +
                        "stains are terribly hard to wash out of a beard.",
                )
            }
            else -> {
                chatPlayer(neutral, "I don't have anything suitable to toast with.")
                chatNpc(neutral, "Then go get something from the bar!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Kjut
    private suspend fun Dialogue.kjut() {
        chatNpc(happy, "Kebabs! Get your kebabs here! One gold each!")
        val option =
            choice3(
                "Sure, one kebab please.",
                1,
                "I'm looking for a drunken dwarf.",
                2,
                "Do you have any quests?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Sure, one kebab please.")
                if (!buy(1, "obj.kebab")) {
                    chatNpc(quiz, "Yes? Are you going to give me that one gold coin then?")
                    chatPlayer(sad, "Sorry, I don't seem to have any money on me.")
                    chatNpc(neutral, "The bank is Keldagrim-West. Hurry back!")
                }
            }
            2 -> kjutDrunkenDwarf()
            else -> {
                chatPlayer(quiz, "Do you have any quests?")
                chatNpc(
                    neutral,
                    "Not really! I have a steady supply of meat coming in through the carts " +
                        "and I don't really need anything else!",
                )
            }
        }
    }

    // TODO(content): [quest:forgettable_tale] "I thought you would know plenty!" after completion
    private suspend fun Dialogue.kjutDrunkenDwarf() {
        chatPlayer(quiz, "I'm looking for a drunken dwarf.")
        chatNpc(laugh, "Just go out in the streets, they can't be hard to find!")
        chatPlayer(neutral, "No, a specific drunken dwarf, he keeps harassing me.")
        chatNpc(
            neutral,
            "That's still not very helpful. But let me see. There is one particularly drunken " +
                "dwarf just to the northeast of here.",
        )
        chatNpc(
            angry,
            "He's scaring away my customers, he says there's something wrong with my kebabs. " +
                "Quite annoying fellow.",
        )
        chatPlayer(neutral, "I don't think that's who I mean.")
        chatNpc(
            neutral,
            "Well, he does have a relative who wanders all over Gielinor. Comes back from time " +
                "to time to buy more kebabs. Don't see him very often though.",
        )
        chatPlayer(quiz, "Do you think I'll have any chance of catching him here?")
        chatNpc(
            neutral,
            "I think he was back here quite recently, so I doubt he'll be back any time soon.",
        )
    }

    private fun Dialogue.buy(price: Int, item: String): Boolean {
        if (!player.invTakeFee(price)) {
            return false
        }
        player.invAddOrDrop(objRepo, item)
        return true
    }

    private companion object {
        const val COINS = "obj.coins"
        const val BEER = "obj.beer"
        const val POT = "obj.pot_empty"
        const val YEAST_PRICE = 25

        val TOAST_DRINKS = listOf(BEER, "obj.dwarven_stout")
    }
}
