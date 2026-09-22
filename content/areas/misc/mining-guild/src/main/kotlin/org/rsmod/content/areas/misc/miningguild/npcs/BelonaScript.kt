package org.rsmod.content.areas.misc.miningguild.npcs

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.script.onOpNpc4
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BelonaScript
@Inject
constructor(private val shops: Shops, private val shopOps: ShopOperationMap) : PluginScript() {
    private val Player.faladorEliteDiaryComplete by boolVarBit("varbit.falador_diary_elite_complete")

    override fun ScriptContext.startup() {
        shopOps.costOf(CURRENCY) { type ->
            FIXED_PRICES[RSCM.getReverseMapping(RSCMType.OBJ, type.id)] ?: type.cost.coerceAtLeast(1)
        }
        onOpNpc1(BELONA) { startDialogue(it.npc) { greeting() } }
        onOpNpc3(BELONA) { player.openMineralExchange() }
        onOpNpc4(BELONA) { startDialogue(it.npc) { toggleMinerals(withPlayerLine = false) } }
    }

    private suspend fun Dialogue.greeting() {
        chatNpc(neutral, "Hello human.")
        chatPlayer(quiz, "What do you do here?")
        chatNpc(
            neutral,
            "This guild contains minerals that haven't been found anywhere else. I'm here to try and " +
                "identify them.",
        )
        chatNpc(
            neutral,
            "If you come across any unusual minerals when mining here I'll take them off your hands. " +
                "Don't worry, I'll make it worth your while.",
        )
        chatNpc(neutral, "I've also been digging out this cave recently. I found a bunch of amethyst in there.")
        val toggleText =
            if (player.receivesMinerals) "I don't want to receive minerals." else "I'd like to receive minerals."
        when (
            choice5(
                "I have minerals to trade.",
                1,
                "What can you offer me?",
                2,
                toggleText,
                3,
                "Can you combine my mining gloves?",
                4,
                "That's all.",
                5,
            )
        ) {
            1 -> player.openMineralExchange()
            2 -> offers()
            3 -> toggleMinerals(withPlayerLine = true)
            4 -> combineGloves()
            else -> farewell()
        }
    }

    private suspend fun Dialogue.offers() {
        chatPlayer(quiz, "What can you offer me?")
        chatNpc(
            neutral,
            "I have some gloves that will help you when Mining. When you wear these gloves you'll " +
                "find that some rocks will instantly respawn.",
        )
        chatPlayer(quiz, "How do they do that?")
        chatNpc(happy, "Magic!")
        chatNpc(neutral, "Beyond that, I could consider letting you access this new cave. It seems to contain even more minerals.")
        chatNpc(neutral, "So, what would you like?")
        val choice =
            if (player.attr[AMETHYST_CAVE_ACCESS] == true) {
                choice2("I'd like to view the shop.", 1, "That's all thank you.", 3)
            } else {
                choice3("I'd like to view the shop.", 1, "I'd like access to the cave.", 2, "That's all thank you.", 3)
            }
        when (choice) {
            1 -> player.openMineralExchange()
            2 -> requestCaveAccess()
            else -> farewell()
        }
    }

    private suspend fun Dialogue.requestCaveAccess() {
        chatPlayer(quiz, "I'd like access to the cave.")
        chatNpc(
            neutral,
            "Now hold on. There's a lot of minerals in there, and digging out the cave was hard work. " +
                "Only a true friend of Falador would be allowed in there.",
        )
        if (!player.faladorEliteDiaryComplete) {
            chatNpc(
                neutral,
                "You don't seem like you're quite there yet. Come speak to me again once you have " +
                    "completed the Elite Falador Achievement Diary.",
            )
            return
        }
        player.attr[AMETHYST_CAVE_ACCESS] = true
        chatNpc(
            happy,
            "And you truly are a friend of Falador. Well done on completing the Elite Falador " +
                "Achievement Diary. Go on in, you may access the cave.",
        )
    }

    private suspend fun Dialogue.toggleMinerals(withPlayerLine: Boolean) {
        if (player.receivesMinerals) {
            if (withPlayerLine) {
                chatPlayer(neutral, "I don't want to receive minerals.")
            }
            player.receivesMinerals = false
            chatNpc(
                neutral,
                "Very well. You'll no longer find any minerals when you mine here. If you change your " +
                    "mind just let me know.",
            )
        } else {
            if (withPlayerLine) {
                chatPlayer(neutral, "I'd like to receive minerals.")
            }
            player.receivesMinerals = true
            chatNpc(
                neutral,
                "As you wish. You'll now find minerals when you mine here. Let me know if you want to " +
                    "change this.",
            )
        }
    }

    private suspend fun Dialogue.combineGloves() {
        chatPlayer(quiz, "Can you combine my mining gloves?")
        chatNpc(
            neutral,
            "I guess I could combine some Mining gloves with some Superior mining gloves. These new " +
                "gloves would require 70 Mining to equip.",
        )
        chatNpc(neutral, "It wouldn't be free though. I'd want $COMBINE_COST minerals in return.")
        if (!choice2("That's fine. Combine my gloves.", true, "That's too much.", false)) {
            chatNpc(neutral, "Fair enough. I'll be here if you change your mind.")
            return
        }
        val inv = access.inv
        if (inv.count(GLOVES) == 0 || inv.count(SUPERIOR_GLOVES) == 0) {
            chatNpc(
                neutral,
                "You don't have any gloves for me to combine. You'll need to bring me some Mining " +
                    "gloves and some Superior mining gloves.",
            )
            return
        }
        if (inv.count(MINERALS) < COMBINE_COST) {
            chatNpc(neutral, "You don't have enough minerals. You need at least $COMBINE_COST.")
            return
        }
        if (access.invDel(inv, GLOVES).failure || access.invDel(inv, SUPERIOR_GLOVES).failure) {
            return
        }
        access.invDel(inv, MINERALS, COMBINE_COST)
        access.invAdd(inv, EXPERT_GLOVES)
        chatNpc(
            happy,
            "There you go. I've combined your gloves into some Expert mining gloves. Enjoy.",
        )
    }

    private suspend fun Dialogue.farewell() {
        chatPlayer(neutral, "That's all thank you.")
        chatNpc(neutral, "Don't forget to come to me if you find any minerals.")
    }

    private fun Player.openMineralExchange() {
        shops.open(
            player = this,
            title = "Mining Guild Mineral Exchange",
            shopInv = SHOP_INV,
            buyPercentage = 80.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
            currency = CURRENCY,
        )
    }

    private var Player.receivesMinerals: Boolean
        get() = attr[MINERALS_DISABLED] != true
        set(value) {
            if (value) attr.remove(MINERALS_DISABLED) else attr[MINERALS_DISABLED] = true
        }

    private companion object {
        const val BELONA = "npc.mguild_rewardseller"
        const val SHOP_INV = "inv.mguild_rewardshop"
        const val CURRENCY = "currency.unidentified_minerals"

        const val MINERALS = "obj.mguild_minerals"
        const val GLOVES = "obj.mguild_gloves"
        const val SUPERIOR_GLOVES = "obj.mguild_gloves_superior"
        const val EXPERT_GLOVES = "obj.mguild_gloves_expert"
        const val COMBINE_COST = 40

        val FIXED_PRICES = mapOf("obj.reward_gem_bag_guild" to 20)

        val MINERALS_DISABLED = AttributeKey<Boolean>(persistenceKey = "mguild_minerals_disabled")
        val AMETHYST_CAVE_ACCESS = AttributeKey<Boolean>(persistenceKey = "mguild_amethyst_cave_access")
    }
}
