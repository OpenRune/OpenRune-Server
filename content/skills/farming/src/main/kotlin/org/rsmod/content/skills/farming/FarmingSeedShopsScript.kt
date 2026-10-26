package org.rsmod.content.skills.farming

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingSeedShopsScript @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.kebos_seed_merchant") { startDialogue(it.npc) { amelia() } }
        onOpNpc3("npc.kebos_seed_merchant") { openSeedShop() }
        onOpNpc1("npc.kebos_farming_store") { startDialogue(it.npc) { allanna() } }
        onOpNpc3("npc.kebos_farming_store") { openEquipmentShop() }
        onOpNpc1("npc.kebos_poh_garden_supplier") { startDialogue(it.npc) { gardenSupplier() } }
        onOpNpc3("npc.kebos_poh_garden_supplier") { openGardenShop() }
        onOpNpc1("npc.farming_guild_anima_gardener") { startDialogue(it.npc) { felfiz() } }
        onOpNpc3("npc.hosidius_farmingstore") { openVannahShop() }
    }

    private suspend fun Dialogue.amelia() {
        chatNpc(neutral, "Greetings and welcome to my seed shop. Do you want to buy some seeds?")
        if (choice2("Yes, please.", true, "No, thank you.", false)) {
            access.openSeedShop()
        } else {
            chatPlayer(neutral, "No, thank you.")
        }
    }

    private suspend fun Dialogue.allanna() {
        chatNpc(neutral, "Greetings and welcome to my farming equipment shop. Would you like to buy some equipment?")
        if (choice2("Yes, please.", true, "No, thank you.", false)) {
            access.openEquipmentShop()
        } else {
            chatPlayer(neutral, "No, thank you.")
        }
    }

    private suspend fun Dialogue.gardenSupplier(
        greeting: String = "Do you want to buy some garden plants?",
    ) {
        chatNpc(quiz, greeting)
        when (choice3("Yes please!", 1, "What are these plants for?", 2, "No thanks", 3)) {
            1 -> {
                chatPlayer(happy, "Yes please!")
                access.openGardenShop()
            }
            2 -> {
                chatPlayer(quiz, "What are these plants for?")
                chatNpc(happy, "For planting in your house's garden, of course!")
                chatPlayer(quiz, "How do I do that?")
                chatNpc(
                    neutral,
                    "The same way you make furniture. You'll find plant hotspots in your garden and " +
                        "you just need to have one of my bagged saplings with you when you build at them.",
                )
                chatPlayer(neutral, "Ah, I see.")
                gardenSupplier("So do you want to look at my stock?")
            }
            else -> chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.felfiz() {
        chatNpc(neutral, "Hello tall person.")
        chatPlayer(neutral, "Hello short person.")
        if (!choice2("What's this patch for?", true, "See you later.", false)) {
            chatPlayer(neutral, "See you later.")
            return
        }
        chatPlayer(quiz, "What's this patch for?")
        chatNpc(neutral, "This is an Anima patch, the only one of its kind. Here, you can grow Iasor, Kronos and Attas plants.")
        chatPlayer(quiz, "What are they?")
        chatNpc(neutral, "Three unique plants with a special affinity to the Anima Mundi, the life force of our world.")
        chatNpc(neutral, "When planted, all three of these plants have a unique impact on the Anima Mundi. Their effects will likely be very useful to you when farming.")
        chatPlayer(quiz, "What are the effects?")
        chatNpc(neutral, "Iasor plants will reduce the chance of your other crops becoming diseased, Kronos plants will increase the speed at which they grow and Attas plants will improve the yield when harvesting.")
        chatNpc(neutral, "Choose wisely though, as you can only have one of them at a time.")
        chatNpc(neutral, "Eventually, they'll start to wither and die as their impact on the Anima Mundi is limited. You'll have to plant another when they do.")
        chatPlayer(happy, "Awesome! Thanks for the information.")
    }

    private fun ProtectedAccess.openSeedShop() {
        shops.open(
            player = player,
            title = "Amelia's Seed Shop",
            shopInv = "inv.kebos_farming_seed_shop",
            buyPercentage = 60.0,
            sellPercentage = 100.0,
            changePercentage = 2.0,
        )
    }

    private fun ProtectedAccess.openEquipmentShop() {
        shops.open(
            player = player,
            title = "Allanna's Farming Shop",
            shopInv = "inv.kebos_farming_equipment_shop",
            buyPercentage = 60.0,
            sellPercentage = 100.0,
            changePercentage = 2.0,
        )
    }

    private fun ProtectedAccess.openGardenShop() {
        shops.open(
            player = player,
            title = "Garden Centre",
            shopInv = "inv.kebos_poh_garden_centre",
            buyPercentage = 40.0,
            sellPercentage = 100.0,
            changePercentage = 3.0,
        )
    }

    private fun ProtectedAccess.openVannahShop() {
        shops.open(
            player = player,
            title = "Vannah's Farming Stall",
            shopInv = "inv.skill_guide_farming_special",
            buyPercentage = 70.0,
            sellPercentage = 100.0,
            changePercentage = 1.0,
        )
    }
}
