package org.rsmod.content.areas.city.varlamore.civitas.npcs.shops

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
// TODO(content): [skill:thieving] the bazaar stall owners' "You're the one who stole from me!
//  Guards! Guards!" shout after the player steals from their stall
class FortisShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        for (shop in SHOPS) {
            onOpNpc3(shop.npc) { open(it.npc, shop) }
        }
        onOpNpc1(BAKER.npc) { startDialogue(it.npc) { baker(it.npc) } }
        onOpNpc1(FUR.npc) { startDialogue(it.npc) { furMerchant(it.npc) } }
        onOpNpc1(GEMS.npc) { startDialogue(it.npc) { gemMerchant(it.npc) } }
        onOpNpc1(SILK.npc) { startDialogue(it.npc) { silkMerchant(it.npc) } }
        onOpNpc1(SPICES.npc) { startDialogue(it.npc) { spiceMerchant(it.npc) } }
        onOpNpc1(CRAFTING.npc) { startDialogue(it.npc) { artima(it.npc) } }
        onOpNpc1(BLACKSMITH.npc) { startDialogue(it.npc) { blacksmith(it.npc) } }
        onOpNpc1(FOOD.npc) { startDialogue(it.npc) { cobado(it.npc) } }
        onOpNpc1(SEAMSTRESS.npc) { startDialogue(it.npc) { floria(it.npc) } }
        onOpNpc1(GENERAL.npc) { startDialogue(it.npc) { shopkeeper(it.npc) } }
        onOpNpc1(PUB.npc) { startDialogue(it.npc) { bartender(it.npc) } }
        onOpNpc1(FARMING.npc) { startDialogue(it.npc) { agelus(it.npc) } }
    }

    private fun ProtectedAccess.open(npc: Npc, shop: Shop) {
        shops.open(player, npc, shop.title, shop.inv)
    }

    private fun Dialogue.open(npc: Npc, shop: Shop) {
        access.open(npc, shop)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Baker_(Civitas_illa_Fortis)
    private suspend fun Dialogue.baker(npc: Npc) {
        chatNpc(
            happy,
            "You've tried the rest, but how about the best? Our baked goods won't be beaten!",
        )
        when (choice2("Let's see what you have.", 1, "I'm good.", 2)) {
            1 -> {
                chatPlayer(happy, "Let's see what you have.")
                open(npc, BAKER)
            }
            2 -> {
                chatPlayer(neutral, "I'm good.")
                chatNpc(angry, "Hmph!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Fur_Merchant
    private suspend fun Dialogue.furMerchant(npc: Npc) {
        chatNpc(happy, "Hello there. Want to trade some furs?")
        while (true) {
            val option = choice3("Yes please.", 1, "What furs do you have?", 2, "No thanks.", 3)
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes please.")
                    return open(npc, FUR)
                }
                2 -> {
                    chatPlayer(quiz, "What furs do you have?")
                    chatNpc(
                        happy,
                        "I have a selection of both rare and common furs from all across the " +
                            "kingdom. Also, if you happen to have any furs you've brought from " +
                            "the east, I'd be pretty keen to buy some.",
                    )
                    chatNpc(quiz, "So, want to trade?")
                }
                else -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(
                        happy,
                        "Tetamo! If you change your mind, you know where to find me.",
                    )
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gem_Merchant_(Civitas_illa_Fortis)
    private suspend fun Dialogue.gemMerchant(npc: Npc) {
        chatNpc(happy, "Gems! Get your gems here!")
        when (choice2("Let's trade.", 1, "I'm good, thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Let's trade.")
                open(npc, GEMS)
            }
            2 -> {
                chatPlayer(neutral, "I'm good, thanks.")
                chatNpc(angry, "Then move along please!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Silk_Merchant_(Civitas_illa_Fortis)
    private suspend fun Dialogue.silkMerchant(npc: Npc) {
        chatNpc(happy, "Nilsal. Looking to buy some fine silks?")
        when (choice2("Yes please.", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                open(npc, SILK)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "You're missing out, but suit yourself.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Spice_Merchant
    private suspend fun Dialogue.spiceMerchant(npc: Npc) {
        chatNpc(
            happy,
            "A little bit of spice makes everything taste nice! Come and get it here!",
        )
        when (choice2("Let's trade.", 1, "I'll pass.", 2)) {
            1 -> {
                chatPlayer(happy, "Let's trade.")
                open(npc, SPICES)
            }
            2 -> {
                chatPlayer(neutral, "I'll pass.")
                chatNpc(neutral, "Your loss.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Artima
    private suspend fun Dialogue.artima(npc: Npc) {
        chatNpc(
            happy,
            "Nilsal. Here to do some crafting? I've got some supplies that might help you if so.",
        )
        when (choice2("What do you have?", 1, "Just passing through, thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have?")
                chatNpc(happy, "Have a look.")
                open(npc, CRAFTING)
            }
            2 -> {
                chatPlayer(neutral, "Just passing through, thanks.")
                chatNpc(neutral, "Well come back if you change your mind.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Blacksmith
    // TODO(content): [system:item_repair] repairing items for coins (Repair op and dialogue)
    private suspend fun Dialogue.blacksmith(npc: Npc) {
        chatNpc(happy, "Nilsal. Interested in buying some weapons or armour?")
        while (true) {
            val option =
                choice4(
                    "Yes please.",
                    1,
                    "Do you make these yourself?",
                    2,
                    "Can you repair something for me?",
                    3,
                    "No thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes please.")
                    return open(npc, BLACKSMITH)
                }
                2 -> {
                    chatPlayer(quiz, "Do you make these yourself?")
                    chatNpc(
                        happy,
                        "Of course, and you won't find anything finer than what I offer. I " +
                            "didn't become the official blacksmith to the Royal Family of " +
                            "Varlamore by producing subpar goods.",
                    )
                    chatPlayer(
                        quiz,
                        "You're the official blacksmith? Does that mean you can sell me some " +
                            "armour like the knights around here wear?",
                    )
                    chatNpc(quiz, "Are you a Knight of Varlamore?")
                    chatPlayer(neutral, "No.")
                    chatNpc(
                        neutral,
                        "Than I cannot. However, I still have plenty of other items that might " +
                            "interest you. Would you like to take a look?",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Can you repair something for me?")
                    chatNpc(neutral, "You don't have anything I can repair.")
                    return
                }
                else -> {
                    chatPlayer(neutral, "No thanks.")
                    chatNpc(
                        neutral,
                        "As you wish. You are always welcome to return if you change your mind.",
                    )
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Cobado
    // TODO(content): [system:varlamore_tokens] the token hunt lines (single coin and blueberry
    //  muffin rewards) for players with an orange on their head and the purple or every token
    private suspend fun Dialogue.cobado(npc: Npc) {
        chatNpc(happy, "Welcome to Cobado's Groceries. Feeling hungry?")
        while (true) {
            val option =
                choice3(
                    "Let's see what you have.",
                    1,
                    "Why do you have an orange on your head?",
                    2,
                    "I'm good.",
                    3,
                )
            when (option) {
                1 -> {
                    chatPlayer(happy, "Let's see what you have.")
                    return open(npc, FOOD)
                }
                2 -> {
                    chatPlayer(confused, "Why do you have an orange on your head?")
                    if (ORANGE_HAT in player.worn) {
                        chatNpc(
                            quiz,
                            "You know you've got one on your head as well, right? Why have you " +
                                "done it?",
                        )
                        chatPlayer(confused, "Er... I guess it's fun?")
                        chatNpc(
                            happy,
                            "Exactly! Now we've cleared that up, looking to buy some food.",
                        )
                    } else {
                        chatNpc(quiz, "Why don't you have an orange on your head?")
                        chatPlayer(confused, "Er... Because it's not an item of clothing?")
                        chatNpc(neutral, "Not for you perhaps, but it is for me.")
                        chatPlayer(confused, "Right...")
                        chatNpc(happy, "So anyway, looking to buy some food.")
                    }
                }
                else -> {
                    chatPlayer(neutral, "I'm good.")
                    chatNpc(happy, "Well come back when you're hungry.")
                    return
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Floria
    // TODO(content): [system:leagues] "Have you got any other services?" on Demonic Pacts League
    //  worlds
    private suspend fun Dialogue.floria(npc: Npc) {
        chatNpc(
            happy,
            "Hi there. Looking for something new to wear? You've come to the right place if so.",
        )
        when (choice2("Let's see what you have.", 1, "I'm good, thank you.", 2)) {
            1 -> {
                chatPlayer(happy, "Let's see what you have.")
                open(npc, SEAMSTRESS)
            }
            2 -> {
                chatPlayer(neutral, "I'm good, thank you.")
                chatNpc(happy, "No worries.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Shopkeeper_(Civitas_illa_Fortis)
    // TODO(content): [system:shop_stock] inv.fortis_shop_general_1 has no stock definition in
    //  .data/raw-cache/server/shops, so the store opens empty
    private suspend fun Dialogue.shopkeeper(npc: Npc) {
        chatNpc(happy, "Nilsal. Can I help you?")
        when (choice2("What do you have for sale?", 1, "No thanks.", 2)) {
            1 -> {
                chatPlayer(quiz, "What do you have for sale?")
                chatNpc(happy, "Take a look.")
                open(npc, GENERAL)
            }
            2 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Very well.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Bartender_(The_Flaming_Arrow)
    private suspend fun Dialogue.bartender(npc: Npc) {
        chatNpc(happy, "Welcome to The Flaming Arrow. What can I get for you?")
        when (choice2("Let's see what you have.", 1, "Nothing for me, thanks.", 2)) {
            1 -> {
                chatPlayer(happy, "Let's see what you have.")
                open(npc, PUB)
            }
            2 -> {
                chatPlayer(neutral, "Nothing for me, thanks.")
                chatNpc(neutral, "As you wish.")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Agelus
    private suspend fun Dialogue.agelus(npc: Npc) {
        chatNpc(
            happy,
            "Nilsal, iknami. Welcome to my farm. Are you in need of some farming supplies?",
        )
        when (choice2("Yes please.", 1, "No thank you.", 2)) {
            1 -> {
                chatPlayer(happy, "Yes please.")
                open(npc, FARMING)
            }
            2 -> {
                chatPlayer(neutral, "No thank you.")
                chatNpc(neutral, "As you wish.")
            }
        }
    }

    private class Shop(val npc: String, val title: String, val inv: String)

    private companion object {
        const val ORANGE_HAT = "obj.orange_hat"

        val BAKER = Shop("npc.fortis_shop_baker", "Fortis Baker's Stall", "inv.fortis_shop_baker")
        val FUR = Shop("npc.fortis_shop_fur", "Fortis Fur Stall", "inv.fortis_shop_fur")
        val GEMS = Shop("npc.fortis_shop_gems", "Fortis Gem Stall", "inv.fortis_shop_gems")
        val SILK = Shop("npc.fortis_shop_silk", "Fortis Silk Stall", "inv.fortis_shop_silk")
        val SPICES =
            Shop("npc.fortis_shop_spices", "Fortis Spice Stall", "inv.fortis_shop_spices")
        val CRAFTING =
            Shop(
                "npc.fortis_shop_crafting",
                "Artima's Crafting Supplies",
                "inv.fortis_shop_crafting",
            )
        val BLACKSMITH =
            Shop("npc.fortis_shop_blacksmith", "Fortis Blacksmith", "inv.fortis_shop_blacksmith")
        val FOOD = Shop("npc.fortis_shop_food", "Cobado's Groceries", "inv.fortis_shop_food")
        val SEAMSTRESS =
            Shop("npc.fortis_shop_seamstress", "Floria's Fashion", "inv.fortis_shop_seamstress")
        val GENERAL =
            Shop("npc.fortis_shop_general_1", "Fortis General Store", "inv.fortis_shop_general_1")
        val PUB = Shop("npc.fortis_bartender_1", "The Flaming Arrow", "inv.pub_fortis_1")
        val FARMING =
            Shop("npc.fortis_shop_farming", "Agelus' Farm Shop", "inv.fortis_shop_farming")

        val SHOPS =
            listOf(
                BAKER,
                FUR,
                GEMS,
                SILK,
                SPICES,
                CRAFTING,
                BLACKSMITH,
                FOOD,
                SEAMSTRESS,
                GENERAL,
                PUB,
                FARMING,
            )
    }
}
