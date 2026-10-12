package org.rsmod.content.areas.city.varlamore.aldarin.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class AldarinShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (shop in SHOPS) {
            onOpNpc3(shop.npc) { open(it.npc, shop) }
        }
        onOpNpc1(WINE.npc) { startDialogue(it.npc) { antonius(it.npc) } }
        onOpNpc1(PUB.npc) { startDialogue(it.npc) { bartender(it.npc) } }
        onOpNpc1(FOOD.npc) { startDialogue(it.npc) { faustus(it.npc) } }
        onOpNpc1(GENERAL.npc) { startDialogue(it.npc) { shopkeeper(it.npc) } }
        onOpNpc1(GEMS.npc) { startDialogue(it.npc) { toci(it.npc) } }
        onOpNpc1(MINING.npc) { startDialogue(it.npc) { necto(it.npc) } }
        onOpNpc1(SHIELDS.npc) { startDialogue(it.npc) { ichta(it.npc) } }
        onOpNpc1(MISTROCK_PUB.npc) { startDialogue(it.npc) { mistrockBartender(it.npc) } }
    }

    private fun ProtectedAccess.open(npc: Npc, shop: Shop) {
        shops.open(player, npc, shop.title, shop.inv)
    }

    private fun Dialogue.open(npc: Npc, shop: Shop) {
        access.open(npc, shop)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Antonius
    private suspend fun Dialogue.antonius(npc: Npc) {
        chatNpc(
            happy,
            "Welcome to the Moonrise Brewery and Winery, the oldest and largest producer of " +
                "alcohol in all of Varlamore.",
        )
        while (true) {
            val option =
                choice3(
                    "Could I buy some wine?",
                    1,
                    "Could you tell me more about this place?",
                    2,
                    "I'd better be on my way.",
                    3,
                    title = "Select an option",
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Could I buy some wine?")
                    chatNpc(happy, "Certainly!")
                    return open(npc, WINE)
                }
                2 -> moonriseHistory()
                else -> {
                    chatPlayer(neutral, "I'd better be on my way.")
                    chatNpc(happy, "Do come back soon!")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.moonriseHistory() {
        chatPlayer(quiz, "Could you tell me more about this place?")
        chatNpc(
            happy,
            "Of course! The Moonrise Winery was originally opened over 400 years ago by Bracus " +
                "Ramio.",
        )
        chatNpc(
            happy,
            "While we originally started off as just a winery, the brewing side of the business " +
                "was established by Bracus' grandson, Ludovic, when he created the now famous " +
                "sunbeam ale!",
        )
        chatPlayer(quiz, "So it was a family business? Do the family still own it today?")
        chatNpc(
            sad,
            "Unfortunately not. Ludovic found himself in a tragic and unexpected wine-related " +
                "incident before ever having children himself. After his death, the business was " +
                "taken over by a collective.",
        )
        chatPlayer(quiz, "A collective?")
        chatNpc(
            neutral,
            "That's right. Some of the most influential people all across Varlamore have a stake " +
                "in the Moonrise Brewery and Winery these days, though most prefer to stay " +
                "anonymous.",
        )
        chatPlayer(quiz, "Anonymous? How come?")
        chatNpc(shifty, "Ah, well their reasoning is something only they know.")
        chatPlayer(neutral, "I see. Thanks for the information.")
        chatNpc(happy, "Not at all. Now, is there anything else I can do for you?")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(Sunlight%27s_Sanctum)
    private suspend fun Dialogue.bartender(npc: Npc) {
        chatNpc(happy, "Welcome to Sunlight's Sanctum. What will it be?")
        while (true) {
            val option =
                choice3(
                    "Let's see what you have.",
                    1,
                    "Nice place you have here.",
                    2,
                    "Nothing, thanks.",
                    3,
                    title = "Select an option",
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Let's see what you have.")
                    return open(npc, PUB)
                }
                2 -> nicePlace()
                else -> {
                    chatPlayer(neutral, "Nothing, thanks.")
                    chatNpc(neutral, "Fair enough.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.nicePlace() {
        chatPlayer(happy, "Nice place you have here.")
        chatNpc(
            happy,
            "Thank you. I like to think we have one of the nicest pubs in all of Varlamore, not " +
                "that I can take much credit myself.",
        )
        chatPlayer(quiz, "How come?")
        chatNpc(
            neutral,
            "Well it's the Moonrise Brewery and Winery who own this place. It's only as nice as " +
                "it is because of their continued investments.",
        )
        chatPlayer(quiz, "So I'm guessing they care a lot about this place?")
        chatNpc(
            neutral,
            "Well I suppose it just makes sense for them. Keeping this pub in good condition " +
                "keeps the locals happy, which is probably wise for a company that employs half " +
                "of them.",
        )
        chatNpc(
            neutral,
            "We also make plenty of money from all the tourists. Even before Varlamore opened " +
                "up, Aldarin was a top holiday destination. That's easy money for the owners to " +
                "help fund all their other ventures.",
        )
        chatPlayer(neutral, "I suppose so.")
        chatNpc(
            happy,
            "And on that note, I'm guessing you came in for a drink? What will it be?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Faustus
    private suspend fun Dialogue.faustus(npc: Npc) {
        chatNpc(happy, "Nilsal. Interested in buying some of Aldarin's finest fruit and veg?")
        when (choice2("Yes please.", 1, "No thanks.", 2, title = "Select an option")) {
            1 -> {
                chatPlayer(happy, "Let's see what you have.")
                open(npc, FOOD)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Fair enough.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shopkeeper_(Aldarin)
    private suspend fun Dialogue.shopkeeper(npc: Npc) {
        chatNpc(happy, "What can I do for you, iknami?")
        val option =
            choice2(
                "What do you have for sale?",
                1,
                "Nothing, thanks.",
                2,
                title = "Select an option",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What do you have for sale?")
                chatNpc(happy, "Take a look.")
                open(npc, GENERAL)
            }
            2 -> {
                chatPlayer(neutral, "Nothing, thanks.")
                chatNpc(neutral, "Very well.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Toci
    private suspend fun Dialogue.toci(npc: Npc) {
        chatNpc(happy, "Looking to trade gems? You're in the right place if so.")
        when (choice2("Let's trade.", 1, "No thanks.", 2, title = "Select an option")) {
            1 -> {
                chatPlayer(happy, "Let's trade.")
                open(npc, GEMS)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "In that case, timoiva to you.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Necto
    private suspend fun Dialogue.necto(npc: Npc) {
        chatNpc(
            happy,
            "Hello there! Looking to take advantage of the Mistrock mines? If so, I'm happy to " +
                "buy any ores from you. I can also sell you some tools if needed.",
        )
        while (true) {
            val option =
                choice3(
                    "Let's trade.",
                    1,
                    "What can you tell me about this place?",
                    2,
                    "I'm good, thanks.",
                    3,
                    title = "Select an option",
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Let's trade.")
                    chatNpc(happy, "Great!")
                    return open(npc, MINING)
                }
                2 -> mistrockHistory()
                else -> {
                    chatPlayer(neutral, "I'm good, thanks.")
                    chatNpc(neutral, "Well you know where I am if you change your mind.")
                    return
                }
            }
        }
    }

    private suspend fun Dialogue.mistrockHistory() {
        chatPlayer(quiz, "What can you tell me about this place?")
        chatNpc(
            neutral,
            "Mistrock? It's a mining outpost. It was set up by Cam Torum after you humans went " +
                "and had another of your wars. We lost some good mines and needed to find new " +
                "ones.",
        )
        chatPlayer(quiz, "Why here?")
        chatNpc(
            neutral,
            "It wasn't the first choice, but both humans and dwarves alike were pretty poor " +
                "after the fighting ended, so we all had to learn lessons in how to compromise.",
        )
        chatNpc(
            neutral,
            "Still, it ended up not being too bad in the end. We found there was a reasonable " +
                "supply of coal and iron here, along with some silver and gold.",
        )
        chatNpc(
            neutral,
            "The location ended up being quite appealing for some as well. It makes for a nice " +
                "change of scenery for those that appreciate that sort of thing.",
        )
        chatPlayer(quiz, "So what was here before Mistrock?")
        chatNpc(
            neutral,
            "Nothing much of note. The land here was owned by the Moonrise Brewery and Winery, " +
                "but they found it wasn't much good for growing grapes.",
        )
        chatPlayer(quiz, "So they were happy to give it up?")
        chatNpc(
            neutral,
            "Very happy. They saw an opportunity for a new business relationship. In return for " +
                "giving us the land, we now give them favourable rates on what we mine here.",
        )
        chatPlayer(quiz, "Does a winery have much need for metal?")
        chatNpc(
            neutral,
            "Apparently so. I guess they use it to make and maintain all their fancy brewing and " +
                "winemaking equipment.",
        )
        chatNpc(
            happy,
            "But anyway, enough about that. Are you interested in buying or selling some ores?",
        )
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ichta
    private suspend fun Dialogue.ichta(npc: Npc) {
        chatNpc(
            angry,
            "I don't really care for humans in my shop, so you'd better be buying something.",
        )
        val option =
            choice2(
                "What are you selling?",
                1,
                "I'll be on my way then.",
                2,
                title = "Select an option",
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "What are you selling?")
                chatNpc(neutral, "Shields.")
                open(npc, SHIELDS)
            }
            2 -> {
                chatPlayer(neutral, "I'll be on my way then.")
                chatNpc(angry, "Good.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(Stick_Your_Ore_Inn)
    private suspend fun Dialogue.mistrockBartender(npc: Npc) {
        chatNpc(neutral, "Don't get many humans in Stick Your Ore Inn. Are you here to drink?")
        val option =
            choice2(
                "What are you selling?",
                1,
                "I'll be on my way then.",
                2,
                title = "Select an option",
            )
        when (option) {
            1 -> {
                chatPlayer(happy, "Let's see what you have.")
                chatNpc(neutral, "Fair enough.")
                open(npc, MISTROCK_PUB)
            }
            2 -> {
                chatPlayer(neutral, "I'll be on my way then.")
                chatNpc(
                    neutral,
                    "Well then I'm not really sure what we have for you. Perhaps you should be " +
                        "on your way.",
                )
            }
        }
    }

    private class Shop(val npc: String, val title: String, val inv: String)

    private companion object {
        val WINE = Shop("npc.aldarin_wine_store", "Moonrise Wines", "inv.aldarin_wine_store")
        val PUB = Shop("npc.aldarin_bartender", "Sunlight's Sanctum", "inv.pub_aldarin")
        val FOOD =
            Shop("npc.aldarin_food_store", "Faustus' Fruit and Veg", "inv.aldarin_food_store")
        val GENERAL =
            Shop("npc.aldarin_general_store", "Aldarin General Store", "inv.aldarin_general_store")
        val GEMS = Shop("npc.aldarin_gem_store", "Toci's Gem Store", "inv.aldarin_gem_store")
        val MINING =
            Shop(
                "npc.mistrock_mining_store",
                "Mistrock Mining Supplies",
                "inv.mistrock_mining_store",
            )
        val SHIELDS =
            Shop("npc.mistrock_shield_store", "Shields of Mistrock", "inv.mistrock_shield_store")
        val MISTROCK_PUB = Shop("npc.pub_mistrock", "Stick Your Ore Inn", "inv.pub_mistrock")

        val SHOPS = listOf(WINE, PUB, FOOD, GENERAL, GEMS, MINING, SHIELDS, MISTROCK_PUB)
    }
}
