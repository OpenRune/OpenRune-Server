package org.rsmod.content.skills.crafting.npcs

import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.baseCraftingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.skills.crafting.ownsCraftingHood
import org.rsmod.content.skills.crafting.ownsCraftingSkillcape
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CraftingGuildMasterCrafter @Inject constructor() : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(MASTER_CRAFTER) { startDialogue(it.npc) { caped() } }
        onOpNpc1(MASTER_CRAFTER_CAPELESS) { startDialogue(it.npc) { capeless() } }
        onOpNpc1(MASTER_CRAFTER_YOUNG) { startDialogue(it.npc) { young() } }
    }

    private suspend fun Dialogue.caped() {
        if (player.baseCraftingLvl >= 99) {
            capedAt99()
        } else {
            capedBelow99()
        }
    }

    private suspend fun Dialogue.capedBelow99() {
        chatNpc(
            happy,
            "Hello, and welcome to the Crafting Guild. Accomplished crafters from all over the land " +
                "come here to use our top notch workshops.",
        )
        when (
            choice2(
                "Yes.",
                1,
                "No.",
                2,
                title = "Would you like to ask about a Skillcape of Crafting?",
            )
        ) {
            1 -> {
                chatPlayer(quiz, "Hey, what is that cape you're wearing? I don't recognise it.")
                chatNpc(
                    happy,
                    "This? This is a Skillcape of Crafting. It is a symbol of my ability as master of " +
                        "the Crafting Guild and it provides unlimited teleports here.",
                )
                chatNpc(
                    happy,
                    "If you should ever achieve level 99 Crafting come and talk to me and we'll see if " +
                        "we can sort you out with one.",
                )
            }
            2 -> {}
        }
    }

    private suspend fun Dialogue.capedAt99() {
        chatNpc(
            happy,
            "Hello, and welcome to the Crafting Guild. Accomplished crafters from all over the land " +
                "come here to use our top notch workshops.",
        )

        if (!player.ownsCraftingSkillcape()) {
            purchaseSkillcape()
            return
        }
        when (choice2("Skillcape", 1, "Hood", 2)) {
            1 -> chatNpc(neutral, "You've already got a Skillcape of Crafting!")
            2 -> offerFreeHood()
        }
    }

    private suspend fun Dialogue.purchaseSkillcape() {
        chatPlayer(quiz, "Are you the person I need to talk to about buying a Skillcape of Crafting?")
        chatNpc(
            happy,
            "I certainly am, and I can see that you are definitely talented enough to own one! The " +
                "cape has a built-in teleport back to us at the guild too!",
        )
        chatNpc(
            neutral,
            "Unfortunately, being such a prestigious item, they are appropriately expensive. I'm " +
                "afraid I must ask you for $CRAFTING_CAPE_PRICE gold.",
        )
        when (choice2("$CRAFTING_CAPE_PRICE gold! Are you mad?", 1, "That's fine.", 2)) {
            1 -> {
                chatPlayer(neutral, "$CRAFTING_CAPE_PRICE gold! Are you mad?")
                chatNpc(
                    neutral,
                    "Not at all; there are many other adventurers who would love the opportunity to " +
                        "purchase such a prestigious item! You can find me here if you change your mind.",
                )
            }
            2 -> completeSkillcapePurchase()
        }
    }

    private suspend fun Dialogue.completeSkillcapePurchase() {
        if (access.inv.count("obj.coins") < CRAFTING_CAPE_PRICE) {
            chatPlayer(neutral, "That's fine.")
            chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
            chatNpc(neutral, "Well, come back and see me when you do.")
            return
        }
        if (access.inv.freeSpace() < 2) {
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of " +
                    "a skill promotion deal; buy one get one free, you know. So you'll need to " +
                    "free up some inventory space before I can sell you one.",
            )
            return
        }
        val coinDel = access.invDel(access.inv, "obj.coins", count = CRAFTING_CAPE_PRICE, strict = true)
        if (coinDel.failure) {
            chatNpc(neutral, "Well, come back and see me when you do.")
            return
        }
        val capeAdd = access.invAdd(access.inv, CRAFTING_CAPE, 1)
        val hoodAdd = access.invAdd(access.inv, CRAFTING_HOOD, 1)
        if (capeAdd.failure || hoodAdd.failure) {
            access.invAdd(access.inv, "obj.coins", CRAFTING_CAPE_PRICE)
            chatNpc(
                neutral,
                "Unfortunately all Skillcapes are only available with a free hood, it's part of " +
                    "a skill promotion deal; buy one get one free, you know. So you'll need to " +
                    "free up some inventory space before I can sell you one.",
            )
            return
        }
        chatPlayer(neutral, "That's fine.")
        chatNpc(happy, "Excellent! Wear that cape with pride my friend.")
    }

    private suspend fun Dialogue.offerFreeHood() {
        chatPlayer(quiz, "May I have another hood for my cape, please?")
        if (player.ownsCraftingHood()) {
            chatNpc(angry, "You've already got one!")
            return
        }
        chatNpc(happy, "Most certainly, and free of charge!")
        if (access.inv.freeSpace() < 1) {
            chatNpc(neutral, "You'll need a free inventory slot before I can hand you the hood.")
            return
        }
        val add = access.invAdd(access.inv, CRAFTING_HOOD, 1)
        if (add.failure) {
            chatNpc(neutral, "You'll need a free inventory slot before I can hand you the hood.")
        }
    }

    private suspend fun Dialogue.capeless() {
        chatNpc(
            happy,
            "Hello, and welcome to the Crafting Guild. Accomplished crafters from all over the land " +
                "come here to use our top notch workshops.",
        )
    }

    private suspend fun Dialogue.young() {
        chatNpc(neutral, "Yeah?")
        chatPlayer(neutral, "Hello.")
        chatNpc(neutral, "Whassup?")
        chatPlayer(quiz, "So... are you here to give crafting tips?")
        chatNpc(neutral, "Dude, do I look like I wanna talk to you?")
        chatPlayer(neutral, "I suppose not.")
        chatNpc(happy, "Right on!")
    }

    private companion object {
        private const val MASTER_CRAFTER = "npc.master_crafter"
        private const val MASTER_CRAFTER_CAPELESS = "npc.master_crafter_2"
        private const val MASTER_CRAFTER_YOUNG = "npc.master_crafter_3"

        private const val CRAFTING_CAPE = "obj.skillcape_crafting"
        private const val CRAFTING_HOOD = "obj.skillcape_crafting_hood"
        private const val CRAFTING_CAPE_PRICE = 99_000
    }
}
