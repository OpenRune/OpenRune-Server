package org.rsmod.content.slayer.rewards

import dev.openrune.types.ItemServerType
import kotlin.math.min
import org.rsmod.api.enums.SlayerItemRewardsEnums.slayer_item_rewards_cost
import org.rsmod.api.enums.SlayerItemRewardsEnums.slayer_item_rewards_ids
import org.rsmod.api.enums.SlayerItemRewardsEnums.slayer_item_rewards_quantities
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.player.protect.ProtectedAccess

internal object SlayerRewardShop {

    fun handleBuyReward(
        access: ProtectedAccess,
        shopIndex: Int,
        itemType: ItemServerType?,
        requestedSets: Int,
        examine: Boolean,
    ) {
        if (examine) {
            if (itemType != null) {
                access.player.objExamine(itemType, 1, 0)
            }
            return
        }

        val type = slayer_item_rewards_ids[shopIndex] ?: return
        if (!checkBuyRequirements(access, type, shopIndex)) return

        val costPerSet = slayer_item_rewards_cost[type] ?: 0
        if (costPerSet <= 0) {
            access.mes("This item cannot be purchased.")
            return
        }

        val points = SlayerRewardsPoints.getPoints(access.player)
        val affordableSets = min(points / costPerSet, requestedSets)
        if (affordableSets <= 0) {
            access.mes("You don't have enough Slayer points to purchase this.")
            return
        }
        if (affordableSets < requestedSets) {
            access.mes("You don't have enough Slayer points to purchase this many.")
        }

        val stackPerSet = slayer_item_rewards_quantities.find { it.key.id == type.id }?.value ?: 1
        val totalItems = affordableSets * stackPerSet
        val totalCost = affordableSets * costPerSet

        if (!SlayerRewardsPoints.spendPoints(access.player, totalCost)) {
            access.mes("You don't have enough Slayer points to purchase this.")
            return
        }

        val result = access.invAdd(access.inv, type.internalName, totalItems, strict = false)
        if (result.err != null) {
            SlayerRewardsPoints.addPoints(access.player, totalCost)
            access.mes("Not enough space in your inventory.")
            return
        }

        SlayerRewardsPoints.syncPoints(access)
    }

    private fun checkBuyRequirements(access: ProtectedAccess, type: ItemServerType, index: Int): Boolean {
        when (index) {
            1, 2 -> {
                val slayer = access.statBase("stat.slayer")
                val ranged = access.statBase("stat.ranged")
                val requiredRanged = if (index == 1) 61 else 50
                if (slayer < 55 || ranged < requiredRanged) {
                    access.mes(
                        "You need a Slayer and a Ranged level of at least 55 and $requiredRanged " +
                            "respectively to purchase ${type.name.lowercase()}.",
                    )
                    return false
                }
            }
            3 -> {
                if (access.statBase("stat.herblore") < 58) {
                    access.mes("You need a Herblore level of at least 58 to buy a herb sack.")
                    return false
                }
                if (access.playerContainsObj("obj.slayer_herb_sack")) {
                    access.mes("You can only own one herb sack at a time!")
                    return false
                }
            }
        }
        return true
    }
}
