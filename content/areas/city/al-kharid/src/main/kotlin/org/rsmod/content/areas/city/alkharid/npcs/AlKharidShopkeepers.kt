package org.rsmod.content.areas.city.alkharid.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The traders around the Al Kharid market and the shops along its east wall. */
class AlKharidShopkeepers @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.gem_trader") { startDialogue(it.npc) { gemTrader(it.npc) } }
        onOpNpc3("npc.gem_trader") { player.openGemShop(it.npc) }

        onOpNpc1("npc.dommik") { startDialogue(it.npc) { dommik(it.npc) } }
        onOpNpc3("npc.dommik") { player.openCraftingShop(it.npc) }

        onOpNpc1("npc.zeke") { startDialogue(it.npc) { zeke(it.npc) } }
        onOpNpc3("npc.zeke") { player.openScimitarShop(it.npc) }

        onOpNpc1("npc.louie_legs") { startDialogue(it.npc) { louieLegs(it.npc) } }
        onOpNpc3("npc.louie_legs") { player.openLegsShop(it.npc) }

        onOpNpc1("npc.ranael") { startDialogue(it.npc) { ranael(it.npc) } }
        onOpNpc3("npc.ranael") { player.openSkirtShop(it.npc) }

        for (staff in GENERAL_STORE_STAFF) {
            onOpNpc1(staff) { startDialogue(it.npc) { generalStore(it.npc) } }
            onOpNpc3(staff) { player.openGeneralStore(it.npc) }
        }
    }

    private fun Player.openGeneralStore(npc: Npc) =
        shops.open(this, npc, "Al Kharid General Store", "inv.generalshop3")

    private fun Player.openGemShop(npc: Npc) = shops.open(this, npc, "Gem Trader.", "inv.gemshop2")

    private fun Player.openCraftingShop(npc: Npc) =
        shops.open(this, npc, "Dommik's Crafting Store.", "inv.craftingshop2")

    private fun Player.openScimitarShop(npc: Npc) =
        shops.open(this, npc, "Zeke's Superior Scimitars.", "inv.scimitarshop")

    private fun Player.openLegsShop(npc: Npc) =
        shops.open(this, npc, "Louie's Armoured Legs Bazaar.", "inv.legsshop")

    private fun Player.openSkirtShop(npc: Npc) =
        shops.open(this, npc, "Ranael's Super Skirt Store.", "inv.skirtshop")

    private suspend fun Dialogue.generalStore(npc: Npc) {
        chatNpc(happy, "Can I help you at all?")
        if (choice2("Yes please. What are you selling?", true, "No thanks.", false)) {
            access.player.openGeneralStore(npc)
        } else {
            chatPlayer(neutral, "No thanks.")
        }
    }

    private suspend fun Dialogue.gemTrader(npc: Npc) {
        chatNpc(neutral, "Good day to you traveller. Would you be interested in buying some gems?")
        if (choice2("Yes please.", true, "No thank you.", false)) {
            access.player.openGemShop(npc)
        } else {
            chatPlayer(neutral, "No thank you.")
        }
    }

    private suspend fun Dialogue.dommik(npc: Npc) {
        chatNpc(happy, "Would you like to buy some crafting equipment?")
        val browse =
            choice2(
                "No thanks; I've got all the Crafting equipment I need.",
                false,
                "Let's see what you've got, then.",
                true,
            )
        if (browse) {
            access.player.openCraftingShop(npc)
        } else {
            chatPlayer(neutral, "No thanks; I've got all the Crafting equipment I need.")
        }
    }

    private suspend fun Dialogue.zeke(npc: Npc) {
        chatNpc(happy, "A thousand greetings to you, friend.")
        val choice =
            choice3(
                "Do you want to trade?",
                1,
                "Nice cloak.",
                2,
                "Could you sell me a dragon scimitar?",
                3,
            )
        when (choice) {
            1 -> {
                chatPlayer(neutral, "Do you want to trade?")
                chatNpc(happy, "Yes, certainly. I deal in scimitars.")
                access.player.openScimitarShop(npc)
            }
            2 -> {
                chatPlayer(happy, "Nice cloak.")
                chatNpc(happy, "Thank you.")
            }
            3 -> dragonScimitar(npc)
        }
    }

    private suspend fun Dialogue.dragonScimitar(npc: Npc) {
        chatPlayer(quiz, "Could you sell me a dragon scimitar?")
        chatNpc(shocked, "A dragon scimitar? A DRAGON scimitar?")
        chatNpc(laugh, "No way, friend!")
        chatNpc(laugh, "The banana-brained nitwits who make them would never dream of selling any to me.")
        chatNpc(neutral, "Seriously, you'll be a monkey's uncle before you'll ever hold a dragon scimitar.")
        if (QuestRequirements.hasCompleted(player, "quest_monkeymadness1")) {
            chatPlayer(neutral, "Hmmm, funny you should say that...")
        } else {
            chatPlayer(neutral, "Oh well, thanks anyway.")
        }
        chatNpc(neutral, "Perhaps you'd like to take a look at my stock?")
        if (choice2("Yes please, Zeke.", true, "Not today, thank you.", false)) {
            access.player.openScimitarShop(npc)
        } else {
            chatPlayer(neutral, "Not today, thank you.")
        }
    }

    private suspend fun Dialogue.louieLegs(npc: Npc) {
        chatNpc(neutral, "Hey, wanna buy some armour?")
        if (choice2("What have you got?", true, "No, thank you.", false)) {
            chatPlayer(quiz, "What have you got?")
            chatNpc(happy, "I provide items to help you keep your legs!")
            access.player.openLegsShop(npc)
        } else {
            chatPlayer(neutral, "No, thank you.")
        }
    }

    private suspend fun Dialogue.ranael(npc: Npc) {
        chatNpc(
            neutral,
            "Do you want to buy any armoured skirts? Designed especially for ladies who like to fight.",
        )
        if (choice2("Yes please.", true, "No thank you, that's not my scene.", false)) {
            access.player.openSkirtShop(npc)
        } else {
            chatPlayer(neutral, "No thank you, that's not my scene.")
        }
    }

    private companion object {
        val GENERAL_STORE_STAFF = listOf("npc.generalshopkeeper3", "npc.generalassistant3")
    }
}
