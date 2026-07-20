package org.rsmod.content.skills.shootingstars.scripts

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.content.skills.shootingstars.ALL_TIME_TOTAL_DUST
import org.rsmod.content.skills.shootingstars.SEEN_SHOOTING_STAR
import org.rsmod.content.skills.shootingstars.ShootingstarsSettings
import org.rsmod.content.skills.shootingstars.shops.StardustShopOperations
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Dusuri dialogue — matches
 * [OSRS transcript](https://oldschool.runescape.wiki/w/Transcript:Dusuri).
 *
 * OpenRune always treats the world as members, so F2P shop refusals are skipped.
 */
class DusuriScript
@Inject
constructor(
    private val shops: Shops,
    private val shopOps: ShopOperationMap,
    private val stardustOps: StardustShopOperations,
) : PluginScript() {
    private var Player.starTraderMet by boolVarBit("varbit.star_trader_met")

    private val settings: ShootingstarsSettings
        get() = ShootingstarsSettings.load()

    override fun ScriptContext.startup() {
        shopOps.register(CURRENCY, stardustOps)
        syncStarTeleportShopStock()

        for (npc in DUSURI_NPCS) {
            onOpNpc1(npc) { talk(it.npc) }
            onOpNpc3(npc) { openShop() }
        }
    }

    @OptIn(UncheckedType::class)
    private fun syncStarTeleportShopStock() {
        val inv = shops.globalInvs.getOrPut(SHOP_INV) { Inventory.create(SHOP_INV) }
        val tabletSlot = inv.indices.firstOrNull { inv[it]?.isType(STAR_TELEPORT) == true }

        if (settings.starTeleportEnabled) {
            if (tabletSlot != null) {
                return
            }
            val empty = inv.indices.firstOrNull { inv[it] == null } ?: return
            inv[empty] = InvObj(STAR_TELEPORT.asRSCM(RSCMType.OBJ), 100)
            return
        }

        if (tabletSlot != null) {
            inv[tabletSlot] = null
        }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { dialogue(npc) }

    private suspend fun Dialogue.dialogue(npc: Npc) {
        if (!player.starTraderMet) {
            introduction(npc)
            return
        }
        greeting(npc)
    }

    private suspend fun Dialogue.introduction(npc: Npc) {
        chatNpc(happy, "Hello human.")
        chatPlayer(quiz, "Hello. What do you do here?")
        chatNpc(
            happy,
            "I'm Dusuri. I specialise in unique minerals that can't be found on Gielinor.",
        )
        chatPlayer(quiz, "Can't be found on Gielinor? Where do they come from then?")
        chatNpc(
            neutral,
            "To be honest, we don't fully know. All we know is that they can be found within " +
                "shooting stars.",
        )

        if (!player.hasSeenShootingStar()) {
            chatPlayer(quiz, "Shooting stars?")
            chatNpc(
                happy,
                "That's right. They fall from the sky from time to time. They can be mined for " +
                    "the unique minerals that I specialise in.",
            )
            chatNpc(
                happy,
                "If you ever come across one, I'd be happy to trade you for any stardust you find.",
            )
            unlockTrade()
            chatPlayer(happy, "I'll bear that in mind.")
            chatNpc(happy, "Good luck out there.")
            return
        }

        chatPlayer(happy, "Oh yes. I've seen those before.")
        chatNpc(
            happy,
            "Wonderful. If you ever get any stardust from them, I'd be happy to trade you for it.",
        )
        unlockTrade()

        if (player.inv.count("obj.star_dust") <= 0) {
            chatPlayer(happy, "I'll bear that in mind.")
            chatNpc(happy, "Good luck out there.")
            return
        }

        when (
            choice2(
                "I have some here. Can we trade?",
                1,
                "I'll bear that in mind.",
                2,
            )
        ) {
            1 -> {
                chatPlayer(happy, "I have some here. Can we trade?")
                openShopFromDialogue()
            }
            2 -> {
                chatPlayer(happy, "I'll bear that in mind.")
                chatNpc(happy, "Good luck out there.")
            }
        }
    }

    private suspend fun Dialogue.greeting(npc: Npc) {
        chatNpc(happy, "Hello human.")
        options(npc)
    }

    private suspend fun Dialogue.options(npc: Npc) {
        when (
            choice4(
                "Can we trade?",
                1,
                "What do you sell?",
                2,
                "What can you tell me about shooting stars?",
                3,
                "I'm off.",
                4,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Can we trade?")
                openShopFromDialogue()
            }
            2 -> whatDoYouSell(npc)
            3 -> aboutStars(npc)
            4 -> chatPlayer(neutral, "I'm off.")
        }
    }

    private suspend fun Dialogue.whatDoYouSell(npc: Npc) {
        chatPlayer(quiz, "What do you sell?")
        chatNpc(
            happy,
            "I have packs of gems and soft clay if they take your fancy. I also have some " +
                "interesting star fragments that seem to be able to recolour certain metals.",
        )
        chatNpc(
            happy,
            "Most notably, I have a unique ring that we found embedded within a star once. It " +
                "seems to have magical properties that we don't fully understand. What we do " +
                "know is that it can improve your skill while mining.",
        )
        options(npc)
    }

    private suspend fun Dialogue.aboutStars(npc: Npc) {
        chatPlayer(quiz, "What can you tell me about shooting stars?")
        chatNpc(
            neutral,
            "Not a lot really. All I know is that they fall from the sky from time to time and " +
                "that you can mine them.",
        )
        chatNpc(
            neutral,
            "If you want to know more, there's an observatory near Ardougne that you might want " +
                "to visit. They've done some research into them.",
        )
        options(npc)
    }

    private suspend fun Dialogue.openShopFromDialogue() {
        chatNpc(happy, "Of course.")
        player.openStarShop()
    }

    private fun Dialogue.unlockTrade() {
        player.starTraderMet = true
    }

    private fun ProtectedAccess.openShop() {
        player.openStarShop()
    }

    private fun Player.openStarShop() {
        syncStarTeleportShopStock()
        shops.open(
            player = this,
            title = "Dusuri's Star Shop",
            shopInv = SHOP_INV,
            buyPercentage = 80.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
            currency = CURRENCY,
        )
    }

    private fun Player.hasSeenShootingStar(): Boolean =
        (attr[ALL_TIME_TOTAL_DUST] ?: 0) > 0 || attr[SEEN_SHOOTING_STAR] == true

    companion object {
        private const val CURRENCY = "currency.stardust"
        private const val SHOP_INV = "inv.magictraining_inventory"
        private const val STAR_TELEPORT = "obj.poh_tablet_shootingstar"
        private val DUSURI_NPCS =
            listOf("npc.star_trader", "npc.star_trader_1op", "npc.star_trader_2op")
    }
}
