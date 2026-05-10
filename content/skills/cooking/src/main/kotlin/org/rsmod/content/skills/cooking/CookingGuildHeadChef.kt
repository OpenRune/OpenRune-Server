package org.rsmod.content.skills.cooking

import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseCookingLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.game.entity.Npc
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class CookingGuildHeadChef @Inject constructor() : PluginScript() {

    override fun ScriptContext.startup() {
        onOpNpc1(NpcServerType(id = HEAD_CHEF_NPC_ID)) { startDialogue(it.npc) }
    }

    private suspend fun ProtectedAccess.startDialogue(npc: Npc) {
        startDialogue(npc) { headChefDialogue() }
    }

    private suspend fun Dialogue.headChefDialogue() {
        if (player.baseCookingLvl >= 99) {
            dialogueWith99Cooking()
        } else {
            dialogueWithout99Cooking()
        }
    }

    private suspend fun Dialogue.dialogueWithout99Cooking() {
        chatNpc(
            happy,
            "Hello, welcome to the Cooking Guild. Only accomplished chefs and cooks are allowed in here. " +
                "Feel free to use any of our facilities.",
        )
        when (
            choice2(
                "Nice cape you're wearing!",
                1,
                "Thanks, bye.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Nice cape you're wearing!")
                chatNpc(
                    happy,
                    "Thank you! It's my most prized possession, it's a Skillcape of Cooking; it shows that I've " +
                        "achieved level 99 Cooking and am one of the best chefs in the land!",
                )
                chatNpc(
                    happy,
                    "If you ever achieve level 99 Cooking you'll get to wear one too and doing so means you'll never " +
                        "burn any food!",
                )
            }
            2 -> {
                chatPlayer(neutral, "Thanks, bye.")
            }
        }
    }

    private suspend fun Dialogue.dialogueWith99Cooking() {
        chatNpc(
            happy,
            "Hello, welcome to the Cooking Guild. It's always great to have such an accomplished chef visit us. Say, " +
                "would you be interested in a Skillcape of Cooking? They're only available to master chefs.",
        )
        when (
            choice2(
                "No thanks.",
                1,
                "Yes please.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(neutral, "No thanks.")
                chatNpc(neutral, "Okay, come back to me if you change your mind.")
            }
            2 -> {
                if (!player.ownsCookingSkillcape()) {
                    requestCookingCapePurchase()
                } else {
                    when (
                        choice2(
                            "Skillcape",
                            1,
                            "Hood",
                            2,
                        )
                    ) {
                        1 -> requestCookingCapePurchase()
                        2 -> offerFreeHood()
                    }
                }
            }
        }
    }

    private suspend fun Dialogue.requestCookingCapePurchase() {
        chatPlayer(quiz, "Can I buy a Skillcape of Cooking from you?")
        chefQuotesCookingCapePriceThenPurchase()
    }

    private suspend fun Dialogue.chefQuotesCookingCapePriceThenPurchase() {
        chatNpc(
            happy,
            "Most certainly, by wearing this cape you'll never burn any food, I will have to ask for 99000 gold " +
                "coins for such a privilege.",
        )
        when (
            choice2(
                "That's much too expensive.",
                1,
                "Sure.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(sad, "That's much too expensive.")
                chatNpc(sad, "I'm sorry you feel that way.")
            }
            2 -> {
                chatPlayer(happy, "Sure.")
                if (access.inv.count("obj.coins") < COOKING_CAPE_PRICE) {
                    chatPlayer(sad, "But, unfortunately, I don't have enough money with me.")
                    chatNpc(neutral, "Well, come back and see me when you do.")
                } else if (access.inv.freeSpace() < 2) {
                    chatNpc(
                        neutral,
                        "Unfortunately all Skillcapes are only available with a free hood, it's part of a skill " +
                            "promotion deal; buy one get one free, you know. So you'll need to free up some " +
                            "inventory space before I can sell you one.",
                    )
                } else {
                    val coinDel =
                        access.invDel(access.inv, "obj.coins", count = COOKING_CAPE_PRICE, strict = true)
                    if (coinDel.failure) {
                        chatNpc(neutral, "Well, come back and see me when you do.")
                        return
                    }
                    val capeAdd = access.invAdd(access.inv, "obj.skillcape_cooking", 1)
                    val hoodAdd = access.invAdd(access.inv, "obj.skillcape_cooking_hood", 1)
                    if (capeAdd.failure || hoodAdd.failure) {
                        access.invAdd(access.inv, "obj.coins", COOKING_CAPE_PRICE)
                        chatNpc(
                            neutral,
                            "Unfortunately all Skillcapes are only available with a free hood, it's part of a skill " +
                                "promotion deal; buy one get one free, you know. So you'll need to free up some " +
                                "inventory space before I can sell you one.",
                        )
                        return
                    }
                    chatNpc(happy, "Now you can use the title Master Chef.")
                }
            }
        }
    }

    private suspend fun Dialogue.offerFreeHood() {
        chatPlayer(quiz, "May I have another hood for my cape, please?")
        chatNpc(happy, "Most certainly, and free of charge!")
        if (access.inv.freeSpace() < 1) {
            chatNpc(neutral, "You'll need a free inventory slot before I can hand you the hood.")
            return
        }
        val add = access.invAdd(access.inv, "obj.skillcape_cooking_hood", 1)
        if (add.failure) {
            chatNpc(neutral, "You'll need a free inventory slot before I can hand you the hood.")
            return
        }
        mesbox("The head chef hands you another hood for your skillcape.")
    }

    private companion object {
        private const val HEAD_CHEF_NPC_ID = 2658
        private const val COOKING_CAPE_PRICE = 99_000
    }
}
