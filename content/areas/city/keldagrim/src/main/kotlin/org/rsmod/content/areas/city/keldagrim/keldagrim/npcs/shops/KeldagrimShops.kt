package org.rsmod.content.areas.city.keldagrim.keldagrim.npcs.shops

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [system:keldagrim_tasks] item requests from the shopkeepers
class KeldagrimShops @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        shop(SANTIRI) { santiri(it) }
        shop(SARO) { saro(it) }
        shop(VIGR) { vigr(it) }
        shop(TATI) { tati(it) }
        shop(AGMUNDI) { agmundi(it) }
        shop(GUNSLIK) { gunslik(it) }
        shop(STONEMASON) { stonemason(it) }
        shop(GULLDAMAR) { gulldamar(it) }
        shop(HERVI) { hervi(it) }
        shop(NOLAR) { nolar(it) }
        shop(RANDIVOR) { randivor(it) }
        shop(VERMUNDI) { vermundi(it) }
        shop(ORDAN) { ordan(it) }
        shop(JORZIK) { jorzik(it) }
    }

    private fun ScriptContext.shop(shop: Shop, talk: suspend Dialogue.(Npc) -> Unit) {
        onOpNpc1(shop.npc) { startDialogue(it.npc) { talk(it.npc) } }
        onOpNpc3(shop.npc) { shops.open(player, it.npc, shop.title, shop.inv) }
    }

    private fun Dialogue.open(shop: Shop, npc: Npc) {
        shops.open(player, npc, shop.title, shop.inv)
    }

    // https://oldschool.runescape.wiki/w/Transcript:Santiri
    private suspend fun Dialogue.santiri(npc: Npc) {
        chatNpc(
            happy,
            "Welcome, human, to the Quality Weapons Shop! Can I interest you in a purchase?",
        )
        if (choice2("Yes, I'm looking for some weapons.", true, "No thanks.", false)) {
            chatPlayer(happy, "Yes, I'm looking for some weapons.")
            open(SANTIRI, npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Saro
    private suspend fun Dialogue.saro(npc: Npc) {
        chatNpc(happy, "Welcome to my store, human! Are you interested in buying anything?")
        if (choice2("Yes, I'm looking for some armour.", true, "No thanks.", false)) {
            chatPlayer(happy, "Yes, I'm looking for some armour.")
            open(SARO, npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vigr
    private suspend fun Dialogue.vigr(npc: Npc) {
        chatNpc(angry, "What do you want, human?")
        chatPlayer(quiz, "Ehm, anything on offer?")
        chatNpc(angry, "Can you wield a warhammer? If not, then go away.")
        if (choice2("Of course I can.", true, "I can, but I won't.", false)) {
            chatPlayer(happy, "Of course I can.")
            open(VIGR, npc)
        } else {
            chatPlayer(neutral, "I can, but I won't.")
            chatNpc(bored, "Yeah, right.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Tati
    private suspend fun Dialogue.tati(npc: Npc) {
        chatNpc(angry, "What'you want?")
        val option =
            choice3(
                "Do you have any pickaxes?",
                1,
                "Do you have any quests?",
                2,
                "Nothing really.",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Do you-")
                chatNpc(angry, "What? Speak up, I can't hear you!")
                chatPlayer(neutral, "Uhm... I'm just looking for some pickaxes! Do you have any?")
                chatNpc(
                    angry,
                    "Do I have any pickaxes? Do I? Of course I do, this is a pickaxe shop, " +
                        "isn't it!",
                )
                open(TATI, npc)
            }
            2 -> {
                chatPlayer(quiz, "Do you-")
                chatNpc(angry, "What? Stop mumbling!")
                chatPlayer(neutral, "I want a quest!")
                chatNpc(
                    angry,
                    "I don't have any lousy quests... I've got someone who's helping me already!",
                )
                chatNpc(
                    angry,
                    "Well, I say helping... he takes his merry time to do his chores, my son " +
                        "does.",
                )
                chatPlayer(quiz, "Then perhaps I can help in some way?")
                chatNpc(bored, "Pfft, I doubt it... maybe when my son fouls up again, but not now.")
            }
            else -> {
                chatPlayer(neutral, "Nothing really.")
                chatNpc(angry, "Then clear off!")
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Agmundi
    private suspend fun Dialogue.agmundi(npc: Npc) {
        chatNpc(bored, "Oh no, not another human... what do you want then?")
        chatPlayer(quiz, "Oh, do you get humans here often?")
        chatNpc(neutral, "Not that often, no, but sometimes.")
        chatNpc(
            neutral,
            "Of course, since you people are too big for dwarven clothes, they typically don't " +
                "stay very long.",
        )
        chatPlayer(quiz, "Why don't you make bigger clothes then?")
        chatNpc(
            neutral,
            "What'd be the point? Besides, I don't make these clothes myself.",
        )
        val option =
            choice3(
                "Who makes these clothes then?",
                1,
                "I still want to buy your clothes.",
                2,
                "So do you have any quests for me?",
                3,
            )
        when (option) {
            1 -> {
                chatPlayer(quiz, "Who makes these clothes then?")
                chatNpc(
                    neutral,
                    "Oh, my sister, she lives in Keldagrim-East. Has a little stall on the " +
                        "other side of the Kelda.",
                )
                chatNpc(
                    neutral,
                    "If she only worked a little harder, like me, she wouldn't have to live in " +
                        "the sewers of the city. Shame really.",
                )
                chatPlayer(shocked, "The sewers? Your sister lives in the sewers?")
                chatNpc(
                    neutral,
                    "Keldagrim-East, such a ghastly place. Not civil, polite and clean like we " +
                        "are in the West.",
                )
                chatPlayer(bored, "Uh-huh.")
            }
            2 -> open(AGMUNDI, npc)
            else -> {
                chatPlayer(quiz, "So do you have any quests for me?")
                chatNpc(confused, "Quests? Why would I have any quests?")
                chatPlayer(neutral, "Oh, just anything to do would be fine.")
                chatNpc(
                    neutral,
                    "No, not right now... maybe I'll have something for you to do later, but " +
                        "nothing at the moment.",
                )
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gunslik
    private suspend fun Dialogue.gunslik(npc: Npc) {
        chatNpc(happy, "What can I interest you in? We have something of everything here!")
        if (choice2("Oh good!", true, "Nothing, thanks.", false)) {
            chatPlayer(happy, "Oh good!")
            open(GUNSLIK, npc)
        } else {
            chatPlayer(neutral, "Nothing, thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Stonemason
    private suspend fun Dialogue.stonemason(npc: Npc) {
        chatNpc(quiz, "Do you want to buy any stone for building? Or gold leaf?")
        while (true) {
            val option =
                choice3("Yes please", 1, "No thanks", 2, "What kind of stone do you sell?", 3)
            when (option) {
                1 -> {
                    open(STONEMASON, npc)
                    return
                }
                2 -> {
                    chatPlayer(neutral, "No thanks.")
                    return
                }
                else -> {
                    chatPlayer(quiz, "What kind of stone do you sell?")
                    chatNpc(
                        neutral,
                        "A few kinds. Limestone, of course. Here in Keldagrim we're surrounded " +
                            "by the stuff so I can practically give it away! You'll be able to " +
                            "build some kinds of furniture out of that.",
                    )
                    chatNpc(
                        neutral,
                        "Marble is much better, of course, but it costs a lot for me to get hold " +
                            "of it. You can make some beautiful pieces of furniture out of that.",
                    )
                    chatNpc(
                        neutral,
                        "I also sell gold leaf, which can be used to decorate furniture made of " +
                            "marble or mahogany.",
                    )
                    chatNpc(
                        neutral,
                        "Then for the very special furniture I sell these magic stones. Only the " +
                            "most skilled builders can work them, but they can create a few " +
                            "amazing pieces of furniture with them.",
                    )
                    chatNpc(quiz, "So, do you want to buy anything?")
                }
            }
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Gulldamar
    private suspend fun Dialogue.gulldamar(npc: Npc) {
        chatNpc(happy, "Finest silver in all of Keldagrim, come and see!")
        if (choice2("Right, what do you have?", true, "Not interested, thanks.", false)) {
            chatPlayer(quiz, "Right, what do you have?")
            chatNpc(happy, "Silver, what else!")
            open(GULLDAMAR, npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Hervi
    private suspend fun Dialogue.hervi(npc: Npc) {
        chatNpc(happy, "Greetings, human... can I interest you in some fine gems?")
        if (choice2("Show me the wares!", true, "No thanks.", false)) {
            chatPlayer(happy, "Show me the wares!")
            open(HERVI, npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Nolar
    private suspend fun Dialogue.nolar(npc: Npc) {
        chatNpc(
            happy,
            "I have a wide variety of crafting tools on offer, care to take a look?",
        )
        if (choice2("Yes please!", true, "No thanks.", false)) {
            chatPlayer(happy, "Yes please!")
            open(NOLAR, npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Randivor
    private suspend fun Dialogue.randivor(npc: Npc) {
        chatNpc(
            happy,
            "Welcome to my stall! Come and buy some tasty bread, freshly baked just 2 months ago!",
        )
        if (choice2("Sounds delicious!", true, "I think I'll pass.", false)) {
            chatPlayer(happy, "Sounds delicious!")
            open(RANDIVOR, npc)
        } else {
            chatPlayer(neutral, "I think I'll pass.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Vermundi
    private suspend fun Dialogue.vermundi(npc: Npc) {
        chatNpc(happy, "Welcome to my clothes stall, can I help you with anything?")
        val browsing =
            choice2(
                "Yes, what clothes do you have in stock?",
                true,
                "No, I'm just browsing.",
                false,
            )
        if (browsing) {
            chatPlayer(quiz, "Yes, what clothes do you have in stock?")
            chatNpc(
                neutral,
                "Not a lot, I'm afraid, most of what I produce goes to my sister. Her shop is in " +
                    "Keldagrim-West.",
            )
            chatPlayer(neutral, "Well, show me what you do have then.")
            open(VERMUNDI, npc)
        } else {
            chatPlayer(neutral, "No, I'm just browsing.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Ordan
    private suspend fun Dialogue.ordan(npc: Npc) {
        chatNpc(quiz, "Are you here to smith? Do you want to buy some ore?")
        if (choice2("Yes please.", true, "No thanks.", false)) {
            open(ORDAN, npc)
        } else {
            chatPlayer(neutral, "No thanks.")
            chatNpc(angry, "Well don't waste my time then!")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Jorzik
    private suspend fun Dialogue.jorzik(npc: Npc) {
        chatNpc(quiz, "Do you want to trade?")
        if (!choice2("What are you selling?", true, "No thanks.", false)) {
            jorzikDecline()
            return
        }
        chatPlayer(quiz, "What are you selling?")
        chatNpc(
            happy,
            "The finest smiths from all over Gielinor come here to work, and I buy the fruit of " +
                "their craft. Armour made from the higher metals!",
        )
        if (choice2("Let's have a look then.", true, "No thanks.", false)) {
            chatPlayer(happy, "Let's have a look then.")
            open(JORZIK, npc)
        } else {
            jorzikDecline()
        }
    }

    private suspend fun Dialogue.jorzikDecline() {
        chatPlayer(neutral, "No thanks.")
        chatNpc(neutral, "You just don't appreciate the beauty of fine metalwork.")
    }

    private data class Shop(val npc: String, val title: String, val inv: String)

    private companion object {
        val SANTIRI =
            Shop(
                "npc.dwarf_city_shop_weapons",
                "Quality Weapons Shop",
                "inv.keldagrim_quality_weapons_shop",
            )
        val SARO =
            Shop(
                "npc.dwarf_city_shop_armour",
                "Quality Armour Shop",
                "inv.keldagrim_quality_armour_shop",
            )
        val VIGR =
            Shop(
                "npc.dwarf_city_shop_warhammer",
                "Vigr's Warhammers",
                "inv.keldagrim_warhammer_shop",
            )
        val TATI =
            Shop(
                "npc.dwarf_city_shop_pickaxe",
                "Pickaxe-Is-Mine",
                "inv.keldagrim_pickaxe_shop",
            )
        val AGMUNDI =
            Shop(
                "npc.dwarf_city_shop_cloth",
                "Agmundi Quality Clothes",
                "inv.keldagrim_clothes_shop",
            )
        val GUNSLIK =
            Shop(
                "npc.dwarf_city_shop_general_store",
                "Gunslik's Assorted Items",
                "inv.keldagrim_general_shop",
            )
        val STONEMASON =
            Shop(
                "npc.poh_stonemason",
                "Keldagrim Stonemason",
                "inv.poh_stonemason_shop",
            )
        val GULLDAMAR =
            Shop(
                "npc.dwarf_city_shop_silver",
                "Silver Cog Silver Stall",
                "inv.keldagrim_silver_stall",
            )
        val HERVI =
            Shop(
                "npc.dwarf_city_shop_gems",
                "Green Gemstone Gems",
                "inv.keldagrim_gem_stall",
            )
        val NOLAR =
            Shop(
                "npc.dwarf_city_shop_craft",
                "Carefree Crafting Stall",
                "inv.keldagrim_crafting_stall",
            )
        val RANDIVOR =
            Shop(
                "npc.dwarf_city_shop_bakery",
                "Keldagrim's Best Bread",
                "inv.keldagrim_bread_stall",
            )
        val VERMUNDI =
            Shop(
                "npc.dwarf_city_shop_cloth_poor",
                "Vermundi's Clothes Stall",
                "inv.keldagrim_clothes_stall",
            )
        val ORDAN =
            Shop(
                "npc.smithing_guild_ore_seller",
                "Ore seller.",
                "inv.smithing_guild_ore_seller",
            )
        val JORZIK = Shop("npc.smithing_guild_buyer", "Armour store.", "inv.smithing_guild_buyer")
    }
}
