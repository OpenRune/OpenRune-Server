package org.rsmod.content.minigames.motherlodemine.scripts

import dev.openrune.rscm.RSCM.asRSCM
import jakarta.inject.Inject
import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.baseMiningLvl
import org.rsmod.api.script.onOpNpc1
import org.rsmod.api.script.onOpNpc3
import org.rsmod.api.shops.Shops
import org.rsmod.api.shops.operation.ShopOperationMap
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeConstants
import org.rsmod.content.minigames.motherlodemine.configs.MotherlodeDelivery.hasBiggerSackUnlocked
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeSuperHopperUnlocked
import org.rsmod.content.minigames.motherlodemine.configs.motherlodeUpperFloorUnlocked
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class PercyScript @Inject constructor(
    private val shops: Shops,
    private val operations: ShopOperationMap,
) : PluginScript() {
    override fun ScriptContext.startup() {
        val prices = Prices.mapKeys { it.key.asRSCM() }
        operations.costOf(MotherlodeConstants.CURRENCY_NUGGET) {
            prices[it.id] ?: it.cost
        }
        onOpNpc1(MotherlodeConstants.NPC_PERCY) { talk(it.npc) }
        onOpNpc3(MotherlodeConstants.NPC_PERCY) { openShop() }
    }

    private suspend fun ProtectedAccess.talk(npc: Npc) = startDialogue(npc) { dialogue(npc) }

    private suspend fun Dialogue.dialogue(npc: Npc) {
        chatNpc(happy, "Hello there! Welcome to the Motherlode Mine.")
        options(npc)
    }

    private suspend fun Dialogue.options(npc: Npc) {
        when (
            choice5(
                "Can we trade?",
                1,
                "I'd like to buy access to the upper floor.",
                2,
                "I'd like to buy the super hopper.",
                3,
                "I'd like to upgrade my sack.",
                4,
                "Never mind.",
                5,
            )
        ) {
            1 -> {
                chatPlayer(happy, "Can we trade?")
                openShopFromDialogue()
            }
            2 -> buyUpperFloor()
            3 -> buySuperHopper()
            4 -> buySackUpgrade()
            5 -> chatPlayer(neutral, "Never mind.")
        }
    }

    private suspend fun Dialogue.buyUpperFloor() {
        if (player.motherlodeUpperFloorUnlocked) {
            chatNpc(happy, "You've already got access to the upper floor.")
            return
        }
        if (player.baseMiningLvl < MotherlodeConstants.UPPER_FLOOR_MINING_REQ) {
            chatNpc(
                neutral,
                "You'll need a Mining level of ${MotherlodeConstants.UPPER_FLOOR_MINING_REQ} " +
                    "before I'll let you up there.",
            )
            return
        }
        if (!confirmPurchase(MotherlodeConstants.UPPER_FLOOR_COST, "access to the upper floor")) {
            return
        }
        player.motherlodeUpperFloorUnlocked = true
        chatNpc(happy, "There you go - the upper floor is all yours now.")
    }

    private suspend fun Dialogue.buySuperHopper() {
        if (player.motherlodeSuperHopperUnlocked) {
            chatNpc(happy, "You've already got the super hopper.")
            return
        }
        if (!player.motherlodeUpperFloorUnlocked) {
            chatNpc(neutral, "You'll need to buy access to the upper floor first.")
            return
        }
        if (!confirmPurchase(MotherlodeConstants.SUPER_HOPPER_COST, "the super hopper")) {
            return
        }
        player.motherlodeSuperHopperUnlocked = true
        chatNpc(happy, "There you go - the super hopper's all set up for you.")
    }

    private suspend fun Dialogue.buySackUpgrade() {
        if (player.hasBiggerSackUnlocked) {
            chatNpc(happy, "You've already upgraded your sack.")
            return
        }
        if (!confirmPurchase(MotherlodeConstants.SACK_UPGRADE_COST, "a bigger sack")) {
            return
        }
        player.hasBiggerSackUnlocked = true
        chatNpc(happy, "There you go - your sack can hold a lot more now.")
    }

    private suspend fun Dialogue.confirmPurchase(cost: Int, description: String): Boolean {
        if (player.inv.count(MotherlodeConstants.OBJ_NUGGET) < cost) {
            chatNpc(
                neutral,
                "That'll cost you $cost golden nuggets - come back when you've got them.",
            )
            return false
        }
        val confirmed =
            choice2(
                "Yes, please.",
                1,
                "No thanks.",
                2,
                title = "Buy $description for $cost golden nuggets?",
            ) == 1
        if (!confirmed) {
            chatPlayer(neutral, "No thanks.")
            return false
        }
        if (access.invDel(player.inv, MotherlodeConstants.OBJ_NUGGET, cost).failure) {
            return false
        }
        return true
    }

    private suspend fun Dialogue.openShopFromDialogue() {
        chatNpc(happy, "Of course, take a look.")
        player.openMotherlodeShop()
    }

    private fun ProtectedAccess.openShop() {
        player.openMotherlodeShop()
    }

    private fun Player.openMotherlodeShop() {
        shops.open(
            player = this,
            title = "Prospector Percy's Nugget Shop",
            shopInv = MotherlodeConstants.SHOP_INV,
            buyPercentage = 80.0,
            sellPercentage = 100.0,
            changePercentage = 0.0,
            currency = MotherlodeConstants.CURRENCY_NUGGET,
            subtext = Shops.DEFAULT_SUBTEXT,
        )
    }

    private companion object {
        val Prices = linkedMapOf(
            MotherlodeConstants.OBJ_PROSPECTOR_HAT to 40,
            MotherlodeConstants.OBJ_PROSPECTOR_TOP to 60,
            MotherlodeConstants.OBJ_PROSPECTOR_LEGS to 50,
            MotherlodeConstants.OBJ_PROSPECTOR_BOOTS to 30,
            MotherlodeConstants.OBJ_COAL_BAG to 100,
            MotherlodeConstants.OBJ_SOFTCLAY_PACK to 10,
        )
    }
}
