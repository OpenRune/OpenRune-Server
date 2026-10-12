package org.rsmod.content.areas.misc.dwarvenmine.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
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

    // https://oldschool.runescape.wiki/w/Transcript:Drogo_dwarf
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

    // https://oldschool.runescape.wiki/w/Transcript:Hura
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

    // https://oldschool.runescape.wiki/w/Transcript:Dwarf_(Dwarven_shopping_store)
    private suspend fun Dialogue.shopkeeper() {
        chatNpc(neutral, "Can I help you at all?")
        if (choice2("Yes please, what are you selling?", true, "No thanks.", false)) {
            player.openShop(GENERAL_STORE)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nurmof
    private suspend fun Dialogue.nurmof() {
        if (BROKEN_DRAGON_PICKAXE in player.inv) {
            repairDragonPickaxe()
            return
        }
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

    private suspend fun Dialogue.repairDragonPickaxe() {
        chatPlayer(happy, "Hi Nurmof, I found this earlier...")
        chatNpc(
            shocked,
            "Oh, a dragon pickaxe! And a broken one at that. I haven't seen one of those in a " +
                "while.",
        )
        chatPlayer(quiz, "Can you fix it for me? I was told you're the dwarf to see!")
        chatNpc(
            neutral,
            "I did indeed fix pickaxes in the past, many years ago. They were a lot more common " +
                "back then.",
        )
        chatNpc(
            neutral,
            "In fact, this is the first broken pickaxe I've seen in a very long time!",
        )
        chatPlayer(quiz, "Why were there so many broken pickaxes back then?")
        chatNpc(
            neutral,
            "Ore rocks used to be quite unstable. They would randomly explode when you were " +
                "mining them.",
        )
        chatPlayer(shocked, "That sounds terrifying!")
        chatNpc(
            neutral,
            "It was. But then one day, the explosions just... stopped. As if someone flipped an " +
                "off switch somewhere.",
        )
        chatNpc(neutral, "I've not seen a broken pickaxe since then!")
        chatPlayer(quiz, "Will you still be able to fix mine for me?")
        chatNpc(neutral, "I should think so. I'll need to buy some new tools.")
        chatNpc(
            neutral,
            "They're quite expensive, but they will get the job done. The pickaxe will be as " +
                "good as new when I've finished with it.",
        )
        chatNpc(neutral, "The cost will be about...")
        chatNpc(
            neutral,
            "...let me see. I need another one of these, and I can reuse that if I replace the " +
                "drill bit from there...",
        )
        chatNpc(
            neutral,
            "...hmm, and this needs a full clean, oh and I don't have one of those anymore...",
        )
        chatNpc(
            happy,
            "...okay! It'll cost about 2,500,000 GP, give or take a few coins. Would you like me " +
                "to fix it for you?",
        )
        if (choice2("Yes, let's do it.", true, "That's rather expensive...", false, REPAIR_TITLE)) {
            chatPlayer(happy, "Yes, let's do it.")
            payForRepair()
            return
        }
        chatPlayer(worried, "That's rather expensive...")
        chatNpc(
            neutral,
            "It is! But you can't cheap out on quality. If a job's worth doing, do it well.",
        )
        chatNpc(
            neutral,
            "I'm also the only one who can repair pickaxes these days, so you don't really have " +
                "much choice!",
        )
        if (choice2("Okay then, let's do it.", true, "I'll think about it.", false, REPAIR_TITLE)) {
            chatPlayer(neutral, "Okay then, let's do it.")
            payForRepair()
            return
        }
        chatPlayer(neutral, "I'll think about it.")
        chatNpc(happy, "I'll be here!")
    }

    private suspend fun Dialogue.payForRepair() {
        chatNpc(
            happy,
            "Lovely! Okay, give me a moment. It may take a little while, it's a tough metal to " +
                "work with.",
        )
        if (!takeRepairPayment()) {
            chatPlayer(
                sad,
                "Oh, I actually don't have enough coins on me! I'll come back with more.",
            )
            chatNpc(happy, "I'll be here!")
            return
        }
        mesbox(
            "Nurmof goes into a nearby hidden room, and from within, you hear the sound of a " +
                "hammer striking metal.",
        )
        mesbox("..he did say it would take a little while.")
        objbox(DRAGON_PICKAXE, "Nurmof hands you the repaired dragon pickaxe.")
        chatNpc(happy, "There you go, good as new, just like I said!")
        chatPlayer(happy, "Thank you very much. I'll take good care of it.")
    }

    private fun Dialogue.takeRepairPayment(): Boolean {
        if (player.inv.count(COINS) < REPAIR_COST) {
            return false
        }
        val result =
            player.invTransaction(player.inv) {
                val inv = select(player.inv)
                delete {
                    from = inv
                    obj = BROKEN_DRAGON_PICKAXE.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
                delete {
                    from = inv
                    obj = COINS.asRSCM(RSCMType.OBJ)
                    strictCount = REPAIR_COST
                }
                insert {
                    into = inv
                    obj = DRAGON_PICKAXE.asRSCM(RSCMType.OBJ)
                    strictCount = 1
                }
            }
        return result.success
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

        const val COINS = "obj.coins"
        const val BROKEN_DRAGON_PICKAXE = "obj.wbr_dragon_pickaxe_broken"
        const val DRAGON_PICKAXE = "obj.dragon_pickaxe"
        const val REPAIR_COST = 2_500_000
        const val REPAIR_TITLE = "Pay Nurmof 2,500,000 GP to fix your dragon pickaxe?"

        val MINING_EMPORIUM = ShopInfo("Drogo's Mining Emporium.", "inv.miningstore", 100.0, 70.0, 3.0)
        val CROSSBOW_SHOP = ShopInfo("Crossbow Shop", "inv.xbows_shop", 360.0, 50.0, 2.0)
        val GENERAL_STORE = ShopInfo("Dwarven shopping store", "inv.generaldwarf", 130.0, 30.0, 3.0)
        val PICKAXE_SHOP = ShopInfo("Nurmof's Pickaxe Shop.", "inv.pickaxeshop", 100.0, 60.0, 2.0)
    }
}
