package org.rsmod.content.areas.misc.guilds.farming

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
class FarmingGuild @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.farming_guild_master") { startDialogue(it.npc) { jane() } }
        onOpNpc1("npc.farming_guild_hespori_farmer") { startDialogue(it.npc) { arno() } }
        onOpNpc1("npc.farming_guild_anima_gardener") { startDialogue(it.npc) { felfiz() } }
        onOpNpc1("npc.kebos_seed_merchant") { startDialogue(it.npc) { amelia(it.npc) } }
        onOpNpc3("npc.kebos_seed_merchant") { player.openSeedShop(it.npc) }
        onOpNpc1("npc.kebos_farming_store") { startDialogue(it.npc) { allanna(it.npc) } }
        onOpNpc3("npc.kebos_farming_store") { player.openFarmingShop(it.npc) }
        onOpNpc1("npc.kebos_poh_garden_supplier") {
            startDialogue(it.npc) { gardenSupplier(it.npc) }
        }
        onOpNpc3("npc.kebos_poh_garden_supplier") { player.openGardenCentre(it.npc) }
    }

    private fun Player.openSeedShop(npc: Npc) {
        shops.open(this, npc, "Amelia's Seed Shop", "inv.kebos_farming_seed_shop")
    }

    private fun Player.openFarmingShop(npc: Npc) {
        shops.open(this, npc, "Allanna's Farming Shop", "inv.kebos_farming_equipment_shop")
    }

    private fun Player.openGardenCentre(npc: Npc) {
        shops.open(this, npc, "Garden Centre", "inv.poh_garden_centre")
    }

    // https://oldschool.runescape.wiki/w/Transcript:Guildmaster_Jane
    // TODO(content): [system:farming_contracts] the contract options (take, complete, current,
    //  count) and the Contract op on the 2-op variant
    // TODO(content): [system:spirit_seed_exchange] "Can I trade in some spirit seeds?"
    // TODO(content): [verify:in_game] which options the top-level menu shows; the transcript
    //  lists every option without the menu layout
    private suspend fun Dialogue.jane() {
        chatNpc(happy, "Welcome to the farming guild. How may I help you?")
        val option =
            choice4(
                "Tell me about the guild.",
                1,
                "Tell me about yourself.",
                2,
                "Do you have any jobs for me?",
                3,
                "I'm just passing by.",
                4,
            )
        when (option) {
            1 -> janeGuild()
            2 -> {
                chatPlayer(quiz, "Tell me about yourself.")
                chatNpc(happy, "My name is Jane, and I am the founder of the Farming Guild.")
                chatNpc(neutral, "Is there anything else I can help you with?")
                val next =
                    choice3(
                        "Tell me about the guild.",
                        1,
                        "I'd like a farming contract.",
                        2,
                        "That's all, thanks.",
                        3,
                    )
                when (next) {
                    1 -> janeGuild()
                    2 -> chatPlayer(quiz, "I'd like a farming contract.")
                    3 -> chatPlayer(neutral, "That's all, thanks.")
                }
            }
            3 -> {
                chatPlayer(quiz, "Do you have any jobs for me?")
                chatNpc(
                    neutral,
                    "Well we always need to rotate the crops we're growing to stop the soil from " +
                        "tiring due to lack of nutrients. It would be great if you could help " +
                        "out with that.",
                )
                chatPlayer(quiz, "What would I need to do?")
                chatNpc(
                    neutral,
                    "If you're interested, I can give you a farming contract which tells you " +
                        "what to plant within the guild. Complete the contract, and I'll give " +
                        "you some seeds as a thank you.",
                )
                val next =
                    choice2("I'd like a farming contract.", 1, "Thanks for the information.", 2)
                when (next) {
                    1 -> chatPlayer(quiz, "I'd like a farming contract.")
                    2 -> chatPlayer(happy, "Thanks for the information.")
                }
            }
            4 -> {
                chatPlayer(neutral, "I'm just passing by.")
                chatNpc(happy, "Sure, let me know if there's anything I can do for you.")
            }
        }
    }

    private suspend fun Dialogue.janeGuild() {
        chatPlayer(quiz, "Tell me about the guild.")
        chatNpc(
            neutral,
            "After the Shayzien Army cleared out the lizardmen in this area, I founded this " +
                "guild with some fellow Hosidius farmers.",
        )
        chatPlayer(quiz, "Why here? Why not within the safety of Kourend?")
        chatNpc(
            neutral,
            "The heat of the volcano allows us to grow things here that would never survive in " +
                "the climate of Kourend. The discoveries we have made are worth the extra risk.",
        )
        chatPlayer(quiz, "Fair enough, anything else I should know about the guild?")
        chatNpc(
            neutral,
            "The guild is split into three tiers, with only advanced farmers being allowed " +
                "access to the upper echelons.",
        )
        chatNpc(quiz, "Would you like to hear more about one of the tiers?")
        while (true) {
            val option =
                choice4(
                    "Tell me about the beginner tier.",
                    1,
                    "Tell me about the intermediate tier.",
                    2,
                    "Tell me about the advanced tier.",
                    3,
                    "No thanks.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "Tell me about the beginner tier.")
                    chatNpc(
                        neutral,
                        "In the beginner tier you can access farming patches for cacti, " +
                            "allotments, bushes and flowers. You'll also find some shops with " +
                            "useful farming supplies.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Tell me about the intermediate tier.")
                    chatNpc(
                        neutral,
                        "In the intermediate tier you can access farming patches for herbs and " +
                            "trees. You'll also find the anima and hespori patches which are " +
                            "unique to this guild.",
                    )
                }
                3 -> {
                    chatPlayer(quiz, "Tell me about the advanced tier.")
                    chatNpc(
                        neutral,
                        "In the advanced tier you can access farming patches for fruit trees and " +
                            "spirit trees. You'll also find the redwood and celastrus patches " +
                            "which are unique to this guild.",
                    )
                }
                4 -> {
                    chatPlayer(neutral, "No thanks.")
                    return
                }
            }
            chatNpc(quiz, "Would you like to hear more about another tier?")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Arno
    // TODO(content): [boss:hespori] Arno's Item Retrieval Service (Talk-to and the Collect op)
    // TODO(content): [verify:in_game] that varbit.farmguild_hespori_seeds set means the player
    //  opted out of Hespori seed drops
    private suspend fun Dialogue.arno() {
        chatNpc(happy, "Hello there.")
        while (true) {
            val optedOut = player.hesporiSeedsDisabled
            val option =
                choice4(
                    "What's in this cave?",
                    1,
                    "Do you have any of my items?",
                    2,
                    if (optedOut) {
                        "I want to find Hespori seeds when farming."
                    } else {
                        "I don't want to find Hespori seeds when farming."
                    },
                    3,
                    "Goodbye.",
                    4,
                )
            when (option) {
                1 -> {
                    chatPlayer(quiz, "What's in this cave?")
                    chatNpc(
                        neutral,
                        "A unique farming patch. This cave is the only place we've succesfully " +
                            "been able to grow a Hespori.",
                    )
                    chatPlayer(quiz, "What's a Hespori?")
                    chatNpc(
                        neutral,
                        "A rare carnivorous plant originally from Morytania. When grown, the " +
                            "plant becomes conscious and highly aggressive. They are truly " +
                            "fascinating.",
                    )
                    chatPlayer(quiz, "Can I grow one?")
                    chatNpc(
                        neutral,
                        "Of course. Just plant some Hespori seeds in the cave below and it will " +
                            "grow like any other plant. A Hespori is pretty tough so you don't " +
                            "need to worry about it getting diseased.",
                    )
                    chatNpc(
                        neutral,
                        "Do be careful though. A fully grown Hespori can be quite troublesome to " +
                            "deal with.",
                    )
                }
                2 -> {
                    chatPlayer(quiz, "Do you have any of my items?")
                    chatNpc(
                        neutral,
                        "I'm afraid not. If I did have any of your items, but you died before " +
                            "collecting them, they'll now be lost.",
                    )
                    return
                }
                3 -> {
                    if (optedOut) {
                        chatPlayer(neutral, "I want to find Hespori seeds when farming.")
                        chatNpc(
                            neutral,
                            "Very well. You'll now have a chance of finding Hespori seeds when " +
                                "farming.",
                        )
                    } else {
                        chatPlayer(neutral, "I don't want to find Hespori seeds when farming.")
                        chatNpc(
                            neutral,
                            "Very well. You'll no longer find Hespori seeds when farming.",
                        )
                    }
                    player.hesporiSeedsDisabled = !optedOut
                }
                4 -> {
                    chatPlayer(neutral, "Goodbye.")
                    return
                }
            }
            chatNpc(quiz, "Do you need anything else?")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Felfiz_Yaryus
    private suspend fun Dialogue.felfiz() {
        chatNpc(happy, "Hello tall person.")
        chatPlayer(happy, "Hello short person.")
        when (choice2("What's this patch for?", 1, "See you later.", 2)) {
            1 -> {
                chatPlayer(quiz, "What's this patch for?")
                chatNpc(
                    neutral,
                    "This is an Anima patch, the only one of its kind. Here, you can grow Iasor, " +
                        "Kronos and Attas plants.",
                )
                chatPlayer(quiz, "What are they?")
                chatNpc(
                    neutral,
                    "Three unique plants with a special affinity to the Anima Mundi, the life " +
                        "force of our world.",
                )
                chatNpc(
                    neutral,
                    "When planted, all three of these plants have a unique impact on the Anima " +
                        "Mundi. Their effects will likely be very useful to you when farming.",
                )
                chatPlayer(quiz, "What are the effects?")
                chatNpc(
                    neutral,
                    "Iasor plants will reduce the chance of your other crops becoming diseased, " +
                        "Kronos plants will increase the speed at which they grow and Attas " +
                        "plants will improve the yield when harvesting.",
                )
                chatNpc(
                    neutral,
                    "Choose wisely though, as you can only have one of them at a time.",
                )
                chatNpc(
                    neutral,
                    "Eventually, they'll start to wither and die as their impact on the Anima " +
                        "Mundi is limited. You'll have to plant another when they do.",
                )
                chatPlayer(happy, "Awesome! Thanks for the information.")
            }
            2 -> chatPlayer(neutral, "See you later.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Amelia_(Farming_Guild)
    private suspend fun Dialogue.amelia(npc: Npc) {
        chatNpc(happy, "Greetings and welcome to my seed shop. Do you want to buy some seeds?")
        when (choice2("Yes, please.", 1, "No, thank you.", 2)) {
            1 -> player.openSeedShop(npc)
            2 -> chatPlayer(neutral, "No, thank you.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Allanna
    private suspend fun Dialogue.allanna(npc: Npc) {
        chatNpc(
            happy,
            "Greetings and welcome to my farming equipment shop. Would you like to buy some " +
                "equipment?",
        )
        when (choice2("Yes, please.", 1, "No, thank you.", 2)) {
            1 -> player.openFarmingShop(npc)
            2 -> chatPlayer(neutral, "No, thank you.")
        }
    }

    // https://oldschool.runescape.wiki/w/Transcript:Garden_supplier
    private suspend fun Dialogue.gardenSupplier(npc: Npc) {
        chatNpc(happy, "Do you want to buy some garden plants?")
        while (true) {
            val option =
                choice3("Yes please!", 1, "What are these plants for?", 2, "No thanks", 3)
            when (option) {
                1 -> {
                    chatPlayer(happy, "Yes please!")
                    player.openGardenCentre(npc)
                    return
                }
                2 -> {
                    chatPlayer(quiz, "What are these plants for?")
                    chatNpc(neutral, "For planting in your house's garden, of course!")
                    chatPlayer(quiz, "How do I do that?")
                    chatNpc(
                        neutral,
                        "The same way you make furniture. You'll find plant hotspots in your " +
                            "garden and you just need to have one of my bagged saplings with you " +
                            "when you build at them.",
                    )
                    chatPlayer(happy, "Ah, I see.")
                    chatNpc(quiz, "So do you want to look at my stock?")
                }
                3 -> {
                    chatPlayer(neutral, "No thanks.")
                    return
                }
            }
        }
    }
}

private var Player.hesporiSeedsDisabled by boolVarBit("varbit.farmguild_hespori_seeds")
