package org.rsmod.content.areas.misc.dwarvenmine.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DwarvenMineShopkeepersScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1(DROGO) { startDialogue(it.npc) { drogo() } }
        onOpNpc3(DROGO) { player.openShop(MINING_EMPORIUM) }

        onOpNpc1(HURA) { startDialogue(it.npc) { hura() } }
        onOpNpc3(HURA) { player.openShop(CROSSBOW_SHOP) }

        onOpNpc1(SHOPKEEPER) { startDialogue(it.npc) { shopkeeper() } }
        onOpNpc3(SHOPKEEPER) { player.openShop(GENERAL_STORE) }

        onOpNpc1(NURMOF) { startDialogue(it.npc) { nurmof() } }
        onOpNpc3(NURMOF) { player.openShop(PICKAXE_SHOP) }
    }

    private suspend fun Dialogue.drogo() {
        chatNpc(happy, "'Ello, welcome to my Mining shop, friend!")
        when (
            choice3(
                "Do you want to trade?",
                1,
                "Hello, shorty.",
                2,
                "Why don't you ever restock ores and bars?",
                3,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "Do you want to trade?")
                player.openShop(MINING_EMPORIUM)
            }
            2 -> {
                chatPlayer(neutral, "Hello, shorty.")
                chatNpc(angry, "I may be short, but at least I've got manners.")
            }
            else -> {
                chatPlayer(quiz, "Why don't you ever restock ores and bars?")
                chatNpc(neutral, "The only ores and bars I sell are those sold to me.")
            }
        }
    }

    private suspend fun Dialogue.hura() {
        chatNpc(neutral, "'Ello ${player.displayName}.")
        chatPlayer(quiz, "Hello, what's that you've got there?")
        chatNpc(neutral, "A crossbow, are you interested?")
        chatPlayer(neutral, "Maybe, are they any good?")
        chatNpc(laugh, "Are they any good?! They're dwarven engineering at its best!")
        when (
            choice3(
                "How do I make one for myself?",
                1,
                "What about ammo?",
                2,
                "Thanks for telling me. Bye!",
                3,
            )
        ) {
            1 -> huraCrossbowGuide()
            2 -> huraAmmo()
            else -> huraFarewell()
        }
    }

    private suspend fun Dialogue.huraCrossbowGuide() {
        chatPlayer(quiz, "How do I make one for myself?")
        chatNpc(
            neutral,
            "Well, firstly you'll need to chop yourself some wood, then use a knife on the wood to whittle " +
                "out a nice crossbow stock like these here.",
        )
        chatPlayer(neutral, "Wood fletched into stock... check.")
        chatNpc(
            neutral,
            "Then get yourself some metal and a hammer and smith yourself some limbs for the bow, mind that " +
                "you use the right metals and woods though as some wood is too light to use with some metal " +
                "and vice versa.",
        )
        chatPlayer(quiz, "Which goes with which?")
        chatNpc(
            neutral,
            "Wood and Bronze as they're basic materials, Oak and Blurite, Willow and Iron, Steel and Teak, " +
                "Mithril and Maple, Adamantite and Mahogany and finally Runite and Yew.",
        )
        chatPlayer(quiz, "Ok, so I have my stock and a pair of limbs... what now?")
        chatNpc(
            neutral,
            "Simply take a hammer and smack the limbs firmly onto the stock. You'll then need a string, only " +
                "they're not the same as normal bows. You'll need to dry some large animal's meat to get " +
                "sinew, then spin that on a spinning",
        )
        chatNpc(neutral, "wheel, it's the only thing we've found to be strong enough for a crossbow.")
        if (choice2("What about magic logs?", true, "Thanks for telling me. Bye!", false)) {
            chatPlayer(quiz, "What about magic logs?")
            chatNpc(
                neutral,
                "Well.. I don't rightly know... us dwarves don't work with magic, we prefer gold and rock. " +
                    "Much more stable. I guess you could ask the humans at the rangers guild to see if they " +
                    "can do something but I don't want",
            )
            chatNpc(neutral, "anything to do with it!")
        }
        huraFarewell()
    }

    private suspend fun Dialogue.huraAmmo() {
        chatPlayer(quiz, "What about ammo?")
        chatNpc(
            neutral,
            "You can smith yourself lots of different bolts, don't forget to flight them with feathers like " +
                "you do arrows though. You can poison any untipped bolt but there's also the option of " +
                "tipping them with gems then",
        )
        chatNpc(neutral, "enchanting them with runes. This can have some pretty powerful effects.")
        chatPlayer(worried, "Oh my poor bank, how will I store all those?!")
        chatNpc(
            neutral,
            "Find Hirko in Keldagrim, he also sells crossbow parts and I'm sure he has something you can use " +
                "to store bolts in.",
        )
        chatPlayer(happy, "Thanks for the info.")
    }

    private suspend fun Dialogue.huraFarewell() {
        chatPlayer(happy, "Thanks for telling me. Bye!")
        chatNpc(happy, "Take care, straight shooting.")
    }

    private suspend fun Dialogue.shopkeeper() {
        chatNpc(neutral, "Can I help you at all?")
        if (choice2("Yes please, what are you selling?", true, "No thanks.", false)) {
            player.openShop(GENERAL_STORE)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.nurmof() {
        chatNpc(
            happy,
            "Greetings and welcome to my pickaxe shop. Do you want to buy my premium quality pickaxes?",
        )
        when (
            choice3(
                "Yes, please.",
                1,
                "No, thank you.",
                2,
                "Are your pickaxes better than other pickaxes, then?",
                3,
            )
        ) {
            1 -> player.openShop(PICKAXE_SHOP)
            2 -> chatPlayer(neutral, "No, thank you.")
            else -> {
                chatPlayer(quiz, "Are your pickaxes better than other pickaxes, then?")
                chatNpc(
                    happy,
                    "Of course they are! My pickaxes are made of higher grade metal than your ordinary bronze " +
                        "pickaxes, allowing you to mine that ore just a little bit faster than normal.",
                )
            }
        }
    }

    private fun Player.openShop(shop: ShopInfo) {
        shops.open(
            player = this,
            title = shop.title,
            shopInv = shop.inv,
            buyPercentage = shop.buyPercentage,
            sellPercentage = shop.sellPercentage,
            changePercentage = shop.changePercentage,
        )
    }

    private data class ShopInfo(
        val title: String,
        val inv: String,
        val sellPercentage: Double,
        val buyPercentage: Double,
        val changePercentage: Double,
    )

    private companion object {
        const val DROGO = "npc.drogo"
        const val HURA = "npc.xbows_sales_dwarf_mine"
        const val SHOPKEEPER = "npc.dwarven_shopkeeper"
        const val NURMOF = "npc.nurmof"

        val MINING_EMPORIUM = ShopInfo("Drogo's Mining Emporium.", "inv.miningstore", 100.0, 70.0, 3.0)
        val CROSSBOW_SHOP = ShopInfo("Crossbow Shop", "inv.xbows_shop", 360.0, 50.0, 2.0)
        val GENERAL_STORE = ShopInfo("Dwarven shopping store", "inv.generaldwarf", 130.0, 30.0, 3.0)
        val PICKAXE_SHOP = ShopInfo("Nurmof's Pickaxe Shop.", "inv.pickaxeshop", 100.0, 60.0, 2.0)
    }
}
