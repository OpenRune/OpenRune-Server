package org.rsmod.content.areas.misc.dwarvenmine.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.content.quest.manager.QuestRequirements
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Nulodion, the cannon engineer at the north end of the Dwarven Mine. */
class Nulodion @Inject constructor(private val shops: Shops) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.nulodion") { startDialogue(it.npc) { greeting() } }
        onOpNpc3("npc.nulodion") { openShop() }
    }

    private suspend fun Dialogue.greeting() {
        chatPlayer(happy, "Hello.")
        chatNpc(happy, "Hello traveller, how's things?")
        chatPlayer(happy, "Not bad thanks, yourself?")
        chatNpc(neutral, "I'm good, just working hard as usual...")
        mainMenu()
    }

    private suspend fun Dialogue.mainMenu() {
        val choice =
            if (helpedTheDwarves()) {
                choice4(
                    "I was hoping you might sell me a cannon.",
                    1,
                    "I've lost my cannon.",
                    2,
                    "I want to know more about the cannon.",
                    3,
                    "Well, take care of yourself then.",
                    4,
                )
            } else {
                choice2("I want to know more about the cannon.", 3, "Well, take care of yourself then.", 4)
            }
        when (choice) {
            1 -> sellCannon()
            2 -> lostCannon()
            3 -> {
                aboutCannon()
                mainMenu()
            }
            else -> {
                chatPlayer(neutral, "Well, take care of yourself then.")
                chatNpc(happy, "Indeed I will adventurer.")
            }
        }
    }

    private suspend fun Dialogue.sellCannon() {
        chatPlayer(neutral, "I was hoping you might sell me a cannon.")
        chatNpc(
            confused,
            "Hmmmmmm... I shouldn't really, but as you helped us so much, well, I could sort " +
                "something out. I'll warn you though, they don't come cheap!",
        )
        chatPlayer(neutral, "How much?")
        chatNpc(
            neutral,
            "For the full setup, $CANNON_PRICE coins. Or I can sell you the separate parts... " +
                "but it'll cost extra!",
        )
        chatPlayer(shocked, "That's not cheap!")
        when (
            choice4(
                "Okay, I'll take a cannon please.",
                1,
                "Can I look at the separate parts please?",
                2,
                "Have you any ammo or instructions to sell?",
                3,
                "Sorry, that's too much for me.",
                4,
            )
        ) {
            1 -> buyFullSetup()
            2 -> {
                chatPlayer(neutral, "Can I look at the separate parts please?")
                chatNpc(happy, "Of course!")
                access.openShop()
            }
            3 -> {
                chatPlayer(neutral, "Have you any ammo or instructions to sell?")
                chatNpc(happy, "Of course!")
                access.openShop()
            }
            else -> {
                chatPlayer(neutral, "Sorry, that's too much for me.")
                chatNpc(neutral, "Fair enough, it's too much for most of us.")
            }
        }
    }

    private suspend fun Dialogue.buyFullSetup() {
        chatPlayer(neutral, "Okay, I'll take a cannon please.")
        val inv = access.inv
        val payingExactly = inv.count(COINS) == CANNON_PRICE
        val needed = if (payingExactly) FULL_SETUP.size - 1 else FULL_SETUP.size
        if (inv.freeSpace() < needed) {
            chatNpc(
                neutral,
                "Okay. There are four pieces to carry, plus the mould and instruction book, so " +
                    "you'll need to free up some space.",
            )
            return
        }
        if (inv.count(COINS) < CANNON_PRICE) {
            chatNpc(neutral, "Okay, come back when you've got the $CANNON_PRICE coins.")
            return
        }
        chatNpc(neutral, "Okay then, but keep it quiet... This thing's top secret!")
        if (access.invDel(inv, COINS, CANNON_PRICE).failure) {
            return
        }
        for (part in FULL_SETUP) {
            access.invAdd(inv, part)
        }
        mesbox(
            "You give the cannon engineer $CANNON_PRICE coins. He gives you the four parts that " +
                "make the cannon, plus an ammo mould and an instruction manual.",
        )
        chatNpc(neutral, "There you go, you be careful with that thing.")
        chatPlayer(happy, "Will do. Take care, mate.")
        chatNpc(happy, "Take care, adventurer.")
    }

    private suspend fun Dialogue.lostCannon() {
        chatPlayer(sad, "I've lost my cannon.")
        chatNpc(
            neutral,
            "Actually, I'm only allowed to replace cannons that were stolen in action. I'm sorry, " +
                "but you'll have to buy a new set.",
        )
    }

    private suspend fun Dialogue.aboutCannon() {
        chatPlayer(quiz, "I want to know more about the cannon.")
        chatNpc(
            neutral,
            "There's only so much I can tell you, adventurer. We've been working on this little " +
                "beauty for some time now.",
        )
        chatPlayer(quiz, "Is it effective?")
        chatNpc(
            happy,
            "In short bursts it's very effective, the most destructive weapon to date. The cannon " +
                "automatically targets monsters close by. You just have to make the ammo and let rip.",
        )
    }

    private fun Dialogue.helpedTheDwarves(): Boolean =
        QuestRequirements.hasCompleted(player, DWARF_CANNON)

    private fun ProtectedAccess.openShop() {
        shops.open(
            player = player,
            title = SHOP_TITLE,
            shopInv = SHOP_INV,
            buyPercentage = SHOP_BUY,
            sellPercentage = SHOP_SELL,
            changePercentage = SHOP_CHANGE,
        )
    }

    private companion object {
        private const val DWARF_CANNON = "quest_dwarfcannon"
        private const val COINS = "obj.coins"
        private const val CANNON_PRICE = 750000

        private const val SHOP_TITLE = "Multicannon parts for sale"
        private const val SHOP_INV = "inv.mcannonshop"
        private const val SHOP_SELL = 107.0
        private const val SHOP_BUY = 40.0
        private const val SHOP_CHANGE = 1.0

        private val FULL_SETUP =
            listOf(
                "obj.twpart1",
                "obj.twpart2",
                "obj.twpart3",
                "obj.twpart4",
                "obj.ammo_mould",
                "obj.mcannonbook",
            )
    }
}
