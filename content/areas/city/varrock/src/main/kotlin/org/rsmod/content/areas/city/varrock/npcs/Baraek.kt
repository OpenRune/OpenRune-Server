package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.invtx.invAddOrDrop
import org.rsmod.api.invtx.invDel
import org.rsmod.api.invtx.invTakeFee
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.manager.menu
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

// https://oldschool.runescape.wiki/w/Transcript:Baraek
// TODO(content): Correct mesanims. The dialogue is from the wiki transcript.
// TODO(content): [quest:shield_of_arrav] "Can you tell me where I can find the Phoenix Gang?"
// TODO(content): [system:treasure_trails] medium challenge scroll
class Baraek @Inject constructor(private val objRepo: ObjRepository) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.baraek") { startDialogue(it.npc) { baraek() } }
    }

    private suspend fun Dialogue.baraek() {
        val options =
            buildList {
                add("Can you sell me some furs?" to 1)
                add("Hello. I am in search of a quest." to 2)
                if (has(GREY_WOLF_FUR)) {
                    add("Would you like to buy my grey wolf fur?" to 3)
                } else if (has(BEAR_FUR) || has(FUR)) {
                    add("Would you like to buy my fur?" to 4)
                }
            }
        when (menu(options)) {
            1 -> sellFur()
            2 -> {
                chatPlayer(happy, "Hello! I am in search of a quest.")
                chatNpc(laugh, "Sorry kiddo, I'm a fur trader not a damsel in distress.")
            }
            3 -> buyGreyWolfFur()
            4 -> buyFur()
        }
    }

    private suspend fun Dialogue.sellFur() {
        chatPlayer(quiz, "Can you sell me some furs?")
        chatNpc(happy, "Yeah, sure. They're 20 gold coins each.")
        val option =
            choice2("Yeah, okay, here you go.", 1, "20 gold coins? That's an outrage!", 2)
        if (option == 2) {
            chatPlayer(angry, "20 gold coins? That's an outrage!")
            chatNpc(
                neutral,
                "Well, I can't go any cheaper than that mate. I have a family to feed.",
            )
            return
        }
        chatPlayer(happy, "Yeah, OK, here you go.")
        if (coins() >= PRICE) {
            buyBearFur(PRICE)
            return
        }
        chatPlayer(sad, "Oh dear, I don't have enough money!")
        chatNpc(neutral, "Well, my best price is 18 coins.")
        if (choice2("OK, here you go.", 1, "No thanks, I'll leave it.", 2) == 2) {
            chatPlayer(neutral, "No thanks, I'll leave it.")
            chatNpc(neutral, "It's your loss mate.")
            return
        }
        chatPlayer(happy, "OK, here you go.")
        if (coins() < LOWEST_PRICE) {
            chatPlayer(sad, "Oh dear, I don't have that either.")
            chatNpc(
                sad,
                "Well, I can't go any cheaper than that mate. I've got a family to feed.",
            )
            chatPlayer(sad, "Oh well, never mind.")
            return
        }
        buyBearFur(LOWEST_PRICE)
    }

    private suspend fun Dialogue.buyBearFur(price: Int) {
        if (!player.invTakeFee(price)) {
            return
        }
        player.invAddOrDrop(objRepo, BEAR_FUR)
        objbox(BEAR_FUR, "Baraek sells you a fur.")
    }

    private suspend fun Dialogue.buyGreyWolfFur() {
        chatPlayer(quiz, "Would you like to buy my grey wolf fur?")
        chatNpc(shocked, "GREY WOLF FUR??? NOW you're talking!")
        chatNpc(happy, "Grey wolf fur is something of a desirable item to me.")
        chatNpc(happy, "I'll take all you have for 120 gold pieces per fur, does that sound fair?")
        val options =
            buildList {
                add("Yep, sounds fine." to 1)
                add("No! I almost got my throat torn out by a wolf to get this!" to 2)
                if (wearingCharos()) {
                    add(CHARM to 3)
                }
            }
        when (menu(options)) {
            1 -> {
                chatPlayer(happy, "Yep, that sounds fine.")
                buyAll(listOf(GREY_WOLF_FUR), GREY_WOLF_PRICE)
                chatPlayer(happy, "Thanks!")
            }
            2 -> chatPlayer(angry, "No! I almost got my throat torn out by a wolf to get this!")
            3 -> {
                charm()
                val single = count(GREY_WOLF_FUR) == 1
                charmOffer(single, GREY_WOLF_CHARM_PRICE)
                chatPlayer(happy, "Now that's more like it!")
                buyAll(listOf(GREY_WOLF_FUR), GREY_WOLF_CHARM_PRICE)
                chatPlayer(happy, "Thanks!")
            }
        }
    }

    private suspend fun Dialogue.buyFur() {
        chatPlayer(quiz, "Would you like to buy my fur?")
        chatNpc(neutral, "Let's have a look at it.")
        if (has(BEAR_FUR)) {
            objbox(BEAR_FUR, "You hand Baraek your fur to look at.")
            if (count(BEAR_FUR) == 1) {
                chatNpc(
                    neutral,
                    "It's not in the best condition. I guess I could give you 12 coins for it.",
                )
            } else {
                chatNpc(
                    neutral,
                    "It's not in the best condition. I guess I could give you 12 coins for each " +
                        "one.",
                )
            }
        } else {
            objbox(FUR, "You hand Baraek your fur to look at.")
            if (count(FUR) == 1) {
                chatNpc(
                    neutral,
                    "It's not in the best of condition. I guess I could give you 12 coins for it.",
                )
            } else {
                chatNpc(
                    neutral,
                    "It's not in the best of condition. I guess I could give you 12 coins for " +
                        "each one.",
                )
            }
        }
        val options =
            buildList {
                add("Yeah, that'll do." to 1)
                add("I think I'll keep hold of it actually." to 2)
                if (wearingCharos()) {
                    add(CHARM to 3)
                }
            }
        when (menu(options)) {
            1 -> {
                chatPlayer(happy, "Yeah, that'll do.")
                buyAll(listOf(BEAR_FUR, FUR), FUR_PRICE)
                chatPlayer(happy, "Thanks!")
            }
            2 -> {
                chatPlayer(neutral, "I think I'll keep hold of it actually!")
                chatNpc(angry, "Oh ok. Didn't want it anyway!")
            }
            3 -> {
                charm()
                val single =
                    count(BEAR_FUR) == 1 || (count(BEAR_FUR) == 0 && count(FUR) == 1)
                charmOffer(single, FUR_CHARM_PRICE)
                chatPlayer(happy, "Now that's more like it!")
                buyAll(listOf(BEAR_FUR, FUR), FUR_CHARM_PRICE)
                chatPlayer(happy, "Thanks!")
            }
        }
    }

    private suspend fun Dialogue.charm() {
        chatPlayer(
            happy,
            "Are you sure? You don't see quality furs like this too often.",
        )
        chatNpc(neutral, "Mmm, let me have another look.")
    }

    private suspend fun Dialogue.charmOffer(single: Boolean, price: Int) {
        if (single) {
            chatNpc(
                happy,
                "That is a really fine piece of fur. Tell you what, I'll give you $price gold " +
                    "pieces for it.",
            )
        } else {
            chatNpc(
                happy,
                "Those are really fine pieces of fur. Tell you what, I'll give you $price gold " +
                    "pieces each for them.",
            )
        }
    }

    private suspend fun Dialogue.buyAll(furs: List<String>, price: Int) {
        for (fur in furs) {
            while (count(fur) > 0) {
                val taken = player.invDel(player.inv, fur, ignoreVirtualStorage = true)
                if (!taken.success) {
                    return
                }
                player.invAddOrDrop(objRepo, COINS, price)
                delay(1)
            }
        }
    }

    private fun Dialogue.wearingCharos(): Boolean = RING_OF_CHAROS in player.worn

    private fun Dialogue.has(obj: String): Boolean = count(obj) > 0

    private fun Dialogue.count(obj: String): Int = player.inv.physicalCount(obj)

    private fun Dialogue.coins(): Int = access.invTotal(player.inv, COINS)

    private companion object {
        const val COINS = "obj.coins"
        const val BEAR_FUR = "obj.fur"
        const val FUR = "obj.werewolve_fur"
        const val GREY_WOLF_FUR = "obj.grey_wolf_fur"
        const val RING_OF_CHAROS = "obj.ring_of_charos_unlocked"
        const val CHARM = "[Charm] Are you sure? You don't see quality furs like this too often."
        const val PRICE = 20
        const val LOWEST_PRICE = 18
        const val FUR_PRICE = 12
        const val FUR_CHARM_PRICE = 20
        const val GREY_WOLF_PRICE = 120
        const val GREY_WOLF_CHARM_PRICE = 150
    }
}
