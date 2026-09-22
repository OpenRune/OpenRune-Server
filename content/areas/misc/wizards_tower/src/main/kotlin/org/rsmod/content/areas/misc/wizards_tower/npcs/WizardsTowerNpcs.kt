package org.rsmod.content.areas.misc.wizards_tower.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class WizardsTowerNpcs : PluginScript() {
    private var Player.rickOpenedShop by intVarBit("varbit.rick_has_opened_shop")

    override fun ScriptContext.startup() {
        onOpNpc1("npc.traiborn") { startDialogue(it.npc) { traiborn() } }
        onOpNpc1("npc.armourmaking_wizard") { startDialogue(it.npc) { jalarast() } }
        onOpNpc3("npc.armourmaking_wizard") { startDialogue(it.npc) { jalarastShop() } }
        onOpNpc1("npc.telecrystal_wizard") { startDialogue(it.npc) { rick() } }
        onOpNpc3("npc.telecrystal_wizard") { startDialogue(it.npc) { buyCrystals() } }
        onOpNpc1("npc.aluft_gnome_mage") { startDialogue(it.npc) { onglewip() } }
    }

    private suspend fun Dialogue.traiborn() {
        chatNpc(confused, "Ello young thingummywut.")
        val topic =
            menu(
                "What's a thingummywut?" to 0,
                "Teach me to be a mighty and powerful wizard." to 1,
                "I'd better go." to 2,
            )
        when (topic) {
            0 -> {
                chatPlayer(quiz, "What's a thingummywut?")
                chatNpc(confused, "A thingummywut? Where? Where?")
                chatNpc(
                    confused,
                    "Those pesky thingummywuts. They get everywhere. They leave a terrible mess too.",
                )
                val next =
                    menu(
                        "Err you just called me a thingummywut." to true,
                        "Tell me what they look like and I'll mash 'em." to false,
                    )
                if (next) {
                    chatPlayer(confused, "Err you just called me thingummywut.")
                    chatNpc(
                        happy,
                        "You're a thingummywut? I've never seen one up close before. They said I " +
                            "was mad!",
                    )
                    chatNpc(
                        happy,
                        "Now you are my proof! There ARE thingummywuts in this tower. Now where can " +
                            "I find a cage big enough to keep you?",
                    )
                    if (menu("Err I'd better be off really." to true, "They're right, you are mad." to false)) {
                        chatPlayer(confused, "Err I'd better be off really.")
                        chatNpc(
                            happy,
                            "Oh ok, have a good time, and watch out for sheep! They're more cunning " +
                                "than they look.",
                        )
                    } else {
                        chatPlayer(neutral, "They're right, you are mad.")
                        chatNpc(sad, "That's a pity. I thought maybe they were winding me up.")
                    }
                } else {
                    chatPlayer(angry, "Tell me what they look like and I'll mash 'em.")
                    chatNpc(neutral, "Don't be ridiculous. No-one has ever seen one.")
                    chatNpc(
                        confused,
                        "They're invisible, or a myth, or a figment of my imagination. Can't " +
                            "remember which right now.",
                    )
                }
            }
            1 -> {
                chatPlayer(quiz, "Teach me to be a mighty and powerful wizard.")
                chatNpc(
                    neutral,
                    "Wizard eh? You don't want any truck with that sort. They're not to be trusted. " +
                        "That's what I've heard anyways.",
                )
                if (menu("So aren't you a wizard?" to true, "Oh I'd better stop talking to you then." to false)) {
                    chatPlayer(quiz, "So aren't you a wizard?")
                    chatNpc(
                        angry,
                        "How dare you? Of course I'm a wizard. Now don't be so cheeky or I'll turn " +
                            "you into a frog.",
                    )
                } else {
                    chatPlayer(neutral, "Oh I'd better stop talking to you then.")
                    chatNpc(happy, "Cheerio then. It was nice chatting to you.")
                }
            }
            else -> {
                chatPlayer(confused, "I'd better go.")
                chatNpc(happy, "Cheerio then.")
            }
        }
    }

    private suspend fun Dialogue.jalarast() {
        chatNpc(happy, "Hello there, can I help you?")
        val topic =
            menu(
                "What do you do here?" to 0,
                "What's that you're wearing?" to 1,
                "Can you make me some armour please?" to 2,
                "No thanks." to 3,
            )
        when (topic) {
            0 -> {
                chatPlayer(quiz, "What do you do here?")
                chatNpc(happy, "I've been studying the practice of making split-bark armour.")
                val next =
                    menu("Split-bark armour, what's that?" to true, "Can you make me some?" to false)
                if (next) {
                    chatPlayer(quiz, "Split-bark armour, what's that?")
                    splitbarkPitch("Split-bark armour is special armour for mages.")
                } else {
                    makeSome()
                }
            }
            1 -> {
                chatPlayer(quiz, "What's that you're wearing?")
                splitbarkPitch("This is split-bark armour, it's special armour for mages.")
            }
            2 -> {
                chatPlayer(quiz, "Can you make me some armour please?")
                chatNpc(happy, "Certainly, what would you like me to make?")
                jalarastShop()
            }
            else -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.splitbarkPitch(opening: String) {
        chatNpc(
            happy,
            "$opening It's much more resistant to physical attacks than normal robes.",
        )
        chatNpc(
            happy,
            "It's actually very easy for me to make, but I've been having trouble getting hold of " +
                "the pieces.",
        )
        if (menu("Well good luck with that." to true, "Can you make me some?" to false)) {
            chatPlayer(neutral, "Well good luck with that.")
        } else {
            makeSome()
        }
    }

    private suspend fun Dialogue.makeSome() {
        chatPlayer(quiz, "Can you make me some?")
        chatNpc(
            happy,
            "I need bark from a hollow tree, and some fine cloth. Unfortunately both these items " +
                "can only be found in Morytania, especially the cloth which is found in the tombs " +
                "of shades.",
        )
        chatNpc(
            happy,
            "Of course I'd happily sell you some at a discounted price if you bring me those items.",
        )
        val howMuch =
            menu("Okay, guess I'll go looking then!" to false, "Okay, how much do I need?" to true)
        if (!howMuch) {
            chatPlayer(happy, "Okay, guess I'll go looking then!")
            return
        }
        chatPlayer(quiz, "Okay, how much do I need?")
        chatNpc(
            happy,
            "I need 1 piece of each for either gloves or boots, 2 pieces of each for a hat, 3 " +
                "pieces of each for leggings, and 4 pieces of each for a top.",
        )
        chatNpc(
            happy,
            "I'll charge you 1,000 coins for either gloves or boots, 6,000 coins for a hat, 32,000 " +
                "coins for leggings, and 37,000 for a top.",
        )
        chatPlayer(happy, "Okay, guess I'll go looking then!")
    }

    private suspend fun Dialogue.jalarastShop() {
        val piece = menu(Splitbark.entries.map { "Buy ${it.label}" to it })
        val inv = access.inv
        val hasSupplies =
            inv.count(HOLLOW_BARK) >= piece.pieces &&
                inv.count(FINE_CLOTH) >= piece.pieces &&
                inv.count(COINS) >= piece.price
        if (!hasSupplies) {
            val plural = if (piece.pieces == 1) "piece" else "pieces"
            chatNpc(
                neutral,
                "You need ${piece.pieces} $plural of bark, ${piece.pieces} $plural of fine cloth " +
                    "and ${"%,d".format(piece.price)} coins for ${piece.description}.",
            )
            return
        }
        access.invDel(inv, HOLLOW_BARK, piece.pieces)
        access.invDel(inv, FINE_CLOTH, piece.pieces)
        access.invDel(inv, COINS, piece.price)
        access.invAdd(inv, piece.obj)
        chatNpc(happy, "There you go, enjoy your new armour!")
    }

    private suspend fun Dialogue.rick() {
        chatPlayer(neutral, "Hey there.")
        chatNpc(neutral, "Hello.")
        var topic =
            menu(
                "Who are you?" to RickTopic.Who,
                "Can I buy a Escape crystal?" to RickTopic.Buy,
                "Which places can the Escape crystal teleport me out of?" to RickTopic.Where,
                "What are you doing here?" to RickTopic.Doing,
                "Goodbye." to RickTopic.Bye,
            )
        while (true) {
            topic =
                when (topic) {
                    RickTopic.Who -> {
                        rickWho()
                        menu(
                            "What are you doing here?" to RickTopic.Doing,
                            "Do you have anything to sell?" to RickTopic.Sell,
                            "Goodbye." to RickTopic.Bye,
                        )
                    }
                    RickTopic.Doing -> {
                        rickDoing()
                        menu(
                            "Who are you?" to RickTopic.Who,
                            "Do you have anything to sell?" to RickTopic.Sell,
                            "Goodbye." to RickTopic.Bye,
                        )
                    }
                    RickTopic.Sell -> return rickSell()
                    RickTopic.Buy -> {
                        chatPlayer(quiz, "Can I buy an Escape crystal?")
                        chatNpc(happy, "Yes! Of course.")
                        return buyCrystals()
                    }
                    RickTopic.Where -> return crystalPlaces()
                    RickTopic.Bye -> {
                        chatPlayer(neutral, "Goodbye.")
                        chatNpc(neutral, "Bye.")
                        return
                    }
                }
        }
    }

    private suspend fun Dialogue.rickWho() {
        chatPlayer(quiz, "Who are you?")
        chatNpc(
            neutral,
            "My name is Rick. I'm a wizard student. I used to study magical farming, but I don't " +
                "like it anymore.",
        )
        chatPlayer(quiz, "Why don't you like it anymore?")
        chatNpc(
            neutral,
            "It's a bit embarrassing, but... I got my head swapped with a pumpkin. Luckily an " +
                "adventurer came along and helped me get it back in place.",
        )
        chatPlayer(neutral, "That is indeed lucky.")
    }

    private suspend fun Dialogue.rickDoing() {
        chatPlayer(quiz, "What are you doing here?")
        chatNpc(
            neutral,
            "I'm a student here at the tower. I don't know what I'll do now. Maybe I'll go home to " +
                "Ardougne. Or maybe I'll pick up a new magical field to study.",
        )
        chatPlayer(quiz, "What kind of magical fields are we talking about?")
        chatNpc(neutral, "Anything but pumpkin fields.")
    }

    private suspend fun Dialogue.rickSell() {
        chatPlayer(quiz, "Do you have anything to sell?")
        chatNpc(
            happy,
            "As a matter of fact, I do! I was recently doing some research on cabbages. I was able " +
                "to extract their essence and combine it with a t-",
        )
        chatPlayer(
            angry,
            "You're not a very good salesman, are you? Just tell me what you're selling!",
        )
        player.rickOpenedShop = 1
        chatNpc(
            happy,
            "Oh. Right! I have these crystal shards that will let you escape from particularly " +
                "dangerous situations. They only work in certain places. I call them Escape " +
                "crystals.",
        )
        chatPlayer(quiz, "Why would I want that?")
        chatNpc(
            neutral,
            "Sometimes you get caught in places that won't let you teleport out by normal means. " +
                "These crystals are specially crafted to let you escape those scenarios. I've " +
                "recently enhanced them as well!",
        )
        chatNpc(
            neutral,
            "Have you ever found yourself zoning out, and come back to reality to find yourself " +
                "under attack and on the brink of death? Well you can configure them to teleport " +
                "you to safety if !",
        )
        chatNpc(
            neutral,
            "It does mean they are likely to trigger in combat, although I'm working on that. But " +
                "they should let you run through a dangerous area as long as you don't stop in " +
                "the middle.",
        )
        chatNpc(
            neutral,
            "They also now work in any place a ring of life would. Talking about rings of life, " +
                "they can also act just like one if you configure them to be so.",
        )
        chatNpc(happy, "So, would you like to buy one? They cost 75000 gold each!")
        val choice =
            menu(
                "Sure - I'd like to buy a crystal." to 0,
                "Which places can the Escape crystal teleport me out of?" to 1,
                "No way, that's too expensive!" to 2,
            )
        when (choice) {
            0 -> sureBuy()
            1 -> crystalPlaces()
            else -> tooExpensive()
        }
    }

    private suspend fun Dialogue.crystalPlaces() {
        chatPlayer(quiz, "Which places can the Escape crystal teleport me out of?")
        chatNpc(
            neutral,
            "The crystals currently work in the Theatre of Blood, the Chambers of Xeric, " +
                "TzHaar-Ket-Rak's Challenges, the Inferno, Pest Control, Barbarian Assault and " +
                "the Tombs of Amascut.",
        )
        chatNpc(
            neutral,
            "Although you cannot technically take them into the Gauntlet, if you enable the " +
                "feature that teleports you if you zone out and have a crystal on you when " +
                "entering, it will continue to work within.",
        )
        chatNpc(happy, "So, would you like to buy one? They cost 75000 gold each!")
        if (menu("Sure - I'd like to buy a crystal." to true, "No way, that's too expensive!" to false)) {
            sureBuy()
        } else {
            tooExpensive()
        }
    }

    private suspend fun Dialogue.sureBuy() {
        chatPlayer(happy, "Sure - I'd like to buy a crystal.")
        chatNpc(happy, "Wonderful!")
        purchaseCrystals()
    }

    private suspend fun Dialogue.tooExpensive() {
        chatPlayer(angry, "No way, that's too expensive!")
        chatNpc(
            neutral,
            "Well, there's a lot that goes into it to drive the price up that high. But I " +
                "understand why you feel that way.",
        )
    }

    private suspend fun Dialogue.buyCrystals() {
        if (access.inv.count(COINS) < CRYSTAL_PRICE) {
            cantAfford()
            return
        }
        val confirm =
            menu("Yes" to true, "No" to false, title = "Buy an escape crystal for 75000 coins?")
        if (confirm) purchaseCrystals()
    }

    private suspend fun Dialogue.purchaseCrystals() {
        val inv = access.inv
        val affordable = inv.count(COINS) / CRYSTAL_PRICE
        if (affordable == 0) {
            cantAfford()
            return
        }
        val requested = access.countDialog("How many would you like to buy?")
        val amount = minOf(requested, affordable, inv.freeSpace())
        if (amount <= 0) return
        if (access.invDel(inv, COINS, amount * CRYSTAL_PRICE).failure) return
        access.invAdd(inv, ESCAPE_CRYSTAL, amount)
        player.rickOpenedShop = 1
    }

    private suspend fun Dialogue.cantAfford() {
        chatNpc(
            neutral,
            "It looks like you can't afford any crystals. They cost 75000 coins each.",
        )
    }

    private suspend fun Dialogue.onglewip() {
        chatPlayer(quiz, "Do you live here too?")
        chatNpc(
            happy,
            "Oh no, I come from the Gnome Stronghold. I've been sent here by King Narnode to learn " +
                "about human magics.",
        )
        chatPlayer(quiz, "So where's this Gnome Stronghold?")
        chatNpc(
            happy,
            "It's in the North West of the continent - a long way away. You should visit us there " +
                "some time. The food's great, and the company's delightful.",
        )
        chatPlayer(happy, "I'll try and make time for it. Sounds like a nice place.")
        chatNpc(happy, "Well, it's full of gnomes. How much nicer could it be?")
    }

    private enum class Splitbark(
        val label: String,
        val obj: String,
        val pieces: Int,
        val price: Int,
        val description: String,
    ) {
        Helm("Helm", "obj.splitbark_helm", 2, 6_000, "a splitbark helm"),
        Body("Body", "obj.splitbark_body", 4, 37_000, "a splitbark body"),
        Legs("Legs", "obj.splitbark_legs", 3, 32_000, "splitbark legs"),
        Gauntlets("Gauntlets", "obj.splitbark_gauntlets", 1, 1_000, "splitbark gauntlets"),
        Boots("Boots", "obj.splitbark_greaves", 1, 1_000, "splitbark boots"),
    }

    private enum class RickTopic {
        Who,
        Buy,
        Where,
        Doing,
        Sell,
        Bye,
    }

    private companion object {
        const val COINS = "obj.coins"
        const val HOLLOW_BARK = "obj.hollow_bark"
        const val FINE_CLOTH = "obj.fine_cloth"
        const val ESCAPE_CRYSTAL = "obj.gauntlet_escape_crystal"
        const val CRYSTAL_PRICE = 75_000
    }
}
