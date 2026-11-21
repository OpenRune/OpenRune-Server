package org.rsmod.content.areas.city.varrock.npcs

import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.script.onOpNpc1
import org.rsmod.content.quest.area.varrock.shieldofarrav.ShieldOfArravQuest
import org.rsmod.content.quest.manager.menu
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class Baraek
@Inject
constructor(
    private val shieldOfArrav: ShieldOfArravQuest,
    private val objRepo: ObjRepository,
) : PluginScript() {
    override fun ScriptContext.startup() {
        onOpNpc1("npc.baraek") { startDialogue(it.npc) { baraek() } }
    }

    private suspend fun Dialogue.baraek() {
        val options = buildList {
            add("Can you sell me some furs?" to Topic.BuyFur)
            if (shieldOfArrav.hasBaraekTopic(player)) {
                add("Can you tell me where I can find the Phoenix Gang?" to Topic.PhoenixGang)
            }
            add("Hello. I am in search of a quest." to Topic.Quest)
            when {
                GREY_WOLF_FUR in player.inv ->
                    add("Would you like to buy my grey wolf fur?" to Topic.SellGreyWolfFur)
                BEAR_FUR in player.inv || FUR in player.inv ->
                    add("Would you like to buy my fur?" to Topic.SellFur)
            }
        }
        when (menu(options)) {
            Topic.BuyFur -> buyFur()
            Topic.PhoenixGang -> with(shieldOfArrav) { baraekPhoenixTopic() }
            Topic.Quest -> {
                chatPlayer(happy, "Hello! I am in search of a quest.")
                chatNpc(neutral, "Sorry kiddo, I'm a fur trader not a damsel in distress.")
            }
            Topic.SellGreyWolfFur -> sellGreyWolfFur()
            Topic.SellFur -> sellFur()
        }
    }

    private suspend fun Dialogue.buyFur() {
        chatPlayer(quiz, "Can you sell me some furs?")
        chatNpc(neutral, "Yeah, sure. They're 20 gold coins each.")
        val buy = choice2("Yeah, okay, here you go.", true, "20 gold coins? That's an outrage!", false)
        if (!buy) {
            chatPlayer(angry, "20 gold coins? That's an outrage!")
            chatNpc(neutral, "Well, I can't go any cheaper than that mate. I have a family to feed.")
            return
        }
        chatPlayer(neutral, "Yeah, OK, here you go.")
        if (BaraekFurTrade.buy(player, FULL_PRICE, ::dropFur)) {
            objbox(BEAR_FUR, "Baraek sells you a fur.")
            return
        }
        chatPlayer(sad, "Oh dear, I don't have enough money!")
        chatNpc(neutral, "Well, my best price is 18 coins.")
        if (!choice2("OK, here you go.", true, "No thanks, I'll leave it.", false)) {
            chatPlayer(neutral, "No thanks, I'll leave it.")
            chatNpc(neutral, "It's your loss mate.")
            return
        }
        chatPlayer(neutral, "OK, here you go.")
        if (BaraekFurTrade.buy(player, BEST_PRICE, ::dropFur)) {
            objbox(BEAR_FUR, "Baraek sells you a fur.")
            return
        }
        chatPlayer(sad, "Oh dear, I don't have that either.")
        chatNpc(neutral, "Well, I can't go any cheaper than that mate. I've got a family to feed.")
        chatPlayer(sad, "Oh well, never mind.")
    }

    private suspend fun Dialogue.sellGreyWolfFur() {
        chatPlayer(quiz, "Would you like to buy my grey wolf fur?")
        chatNpc(happy, "GREY WOLF FUR??? NOW you're talking!")
        chatNpc(happy, "Grey wolf fur is something of a desirable item to me.")
        chatNpc(
            neutral,
            "I'll take all you have for 120 gold pieces per fur, does that sound fair?",
        )
        val sell =
            choice2(
                "Yep, sounds fine.",
                true,
                "No! I almost got my throat torn out by a wolf to get this!",
                false,
            )
        if (!sell) {
            chatPlayer(angry, "No! I almost got my throat torn out by a wolf to get this!")
            return
        }
        chatPlayer(happy, "Yep, that sounds fine.")
        BaraekFurTrade.sellAll(player, listOf(GREY_WOLF_FUR), GREY_WOLF_FUR_PRICE)
        chatPlayer(happy, "Thanks!")
    }

    private suspend fun Dialogue.sellFur() {
        chatPlayer(quiz, "Would you like to buy my fur?")
        chatNpc(neutral, "Let's have a look at it.")
        val shown = if (BEAR_FUR in player.inv) BEAR_FUR else FUR
        objbox(shown, "You hand Baraek your fur to look at.")
        val many = player.inv.count(BEAR_FUR) + player.inv.count(FUR) > 1
        val condition = if (shown == BEAR_FUR) "best condition" else "best of condition"
        val price = if (many) "12 coins for each one." else "12 coins for it."
        chatNpc(neutral, "It's not in the $condition. I guess I could give you $price")
        if (!choice2("Yeah, that'll do.", true, "I think I'll keep hold of it actually.", false)) {
            chatPlayer(neutral, "I think I'll keep hold of it actually!")
            chatNpc(angry, "Oh ok. Didn't want it anyway!")
            return
        }
        chatPlayer(neutral, "Yeah, that'll do.")
        BaraekFurTrade.sellAll(player, listOf(BEAR_FUR, FUR), FUR_PRICE)
        chatPlayer(happy, "Thanks!")
    }

    private fun dropFur(player: Player) {
        objRepo.add(BEAR_FUR, player.coords, constants.lootdrop_duration, player)
    }

    private enum class Topic {
        BuyFur,
        PhoenixGang,
        Quest,
        SellGreyWolfFur,
        SellFur,
    }

    private companion object {
        const val BEAR_FUR = BaraekFurTrade.BEAR_FUR
        const val FUR = "obj.werewolve_fur"
        const val GREY_WOLF_FUR = "obj.grey_wolf_fur"
        const val FULL_PRICE = 20
        const val BEST_PRICE = 18
        const val FUR_PRICE = 12
        const val GREY_WOLF_FUR_PRICE = 120
    }
}
