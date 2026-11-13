package org.rsmod.content.skills.farming

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.config.constants
import org.rsmod.api.player.stat.baseHerbloreLvl
import org.rsmod.api.player.stat.basePrayerLvl
import org.rsmod.api.player.stat.cookingLvl
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onEvent
import org.rsmod.content.generic.locs.water.WateringCans
import org.rsmod.content.interfaces.collectionlog.CollectionLog
import org.rsmod.content.interfaces.omnishop.OmnishopPurchaseCheck
import org.rsmod.content.interfaces.omnishop.OmnishopPurchaseCompleted
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingTitheRewards @Inject constructor() : PluginScript() {
    override fun ScriptContext.startup() {
        onEvent<OmnishopPurchaseCheck> {
            if (shop != SHOP.asRSCM(RSCMType.DBROW)) return@onEvent
            val stockName = STOCKS.firstOrNull { it.asRSCM(RSCMType.DBROW) == stock } ?: return@onEvent
            when (stockName) {
                "dbrow.tithe_shop_grape_blessing" -> failure = FarmingBologa.purchaseRefusal(player)
                "dbrow.tithe_shop_seed_pack" -> failure = "Tithe seed packs are not available: their white-lily reward weights have not been established."
                "dbrow.tithe_shop_herb_sack" -> {
                    if (player.baseHerbloreLvl < 58) failure = "You need a Herblore level of 58 to buy a herb sack."
                    else if (FarmingBags.herbSack.variants.any { player.hasStored(it) }) failure = "You already have a herb sack."
                    maximum = 1
                }
                "dbrow.tithe_shop_seed_box" -> {
                    if (FarmingBags.seedBox.variants.any { player.hasStored(it) }) failure = "You already have a seed box."
                    maximum = 1
                }
                "dbrow.tithe_shop_watering_can" -> {
                    if (player.hasStored(CAN)) failure = "You already have Gricoller's can."
                    maximum = 1
                }
                "dbrow.tithe_shop_watering_can_recharge" -> {
                    output = null
                    maximum = 1
                    if (!WateringCans.needsRefill(player, CAN)) failure = "You are not carrying a Gricoller's can that needs refilling."
                }
                "dbrow.tithe_shop_autoweed" -> {
                    output = null
                    maximum = 1
                    failure = FarmingAutoWeed.purchaseRefusal(player)
                }
                "dbrow.tithe_shop_autoweed_toggle" -> {
                    output = null
                    maximum = 1
                    if (!FarmingAutoWeed.unlocked(player)) failure = "You have not unlocked auto-weed."
                }
                else -> {
                    if (stockName.startsWith("dbrow.tithe_shop_farmers_")) {
                        val body = if (player.appearance.bodyType == constants.bodytype_a) "male" else "female"
                        val name = obj.internalName.substringBeforeLast('_') + "_$body"
                        output = checkNotNull(ServerCacheManager.getItem(name.asRSCM()))
                    }
                }
            }
        }
        onEvent<OmnishopPurchaseCompleted> {
            if (shop != SHOP.asRSCM(RSCMType.DBROW)) return@onEvent
            when (stock) {
                "dbrow.tithe_shop_watering_can_recharge".asRSCM(RSCMType.DBROW) -> check(WateringCans.refill(player, CAN))
                "dbrow.tithe_shop_autoweed".asRSCM(RSCMType.DBROW) -> FarmingAutoWeed.completePurchase(player)
                "dbrow.tithe_shop_autoweed_toggle".asRSCM(RSCMType.DBROW) -> FarmingAutoWeed.toggle(player)
            }
            CollectionLog.grant(player, obj.id, quantity)
            refreshCanOwnership(player)
        }
    }

    companion object {
        const val SHOP = "dbrow.tithe_shop_data"
        private const val CAN = "obj.zeah_wateringcan"
        private val STOCKS = listOf(
            "dbrow.tithe_shop_farmers_hat", "dbrow.tithe_shop_farmers_torso",
            "dbrow.tithe_shop_farmers_legs", "dbrow.tithe_shop_farmers_feet",
            "dbrow.tithe_shop_grape_blessing", "dbrow.tithe_shop_seed_pack",
            "dbrow.tithe_shop_herb_sack", "dbrow.tithe_shop_seed_box",
            "dbrow.tithe_shop_watering_can", "dbrow.tithe_shop_watering_can_recharge",
            "dbrow.tithe_shop_autoweed", "dbrow.tithe_shop_autoweed_toggle",
        )

        fun refreshCanOwnership(player: Player) {
            VarPlayerIntMapSetter.set(player, "varbit.omnishop_hasitem", if (player.hasStored(CAN)) 1 else 0)
        }

        private fun Player.hasStored(obj: String): Boolean = invMap.values.any { it.physicalCount(obj) > 0 }
    }
}

object FarmingBologa {
    fun unlocked(player: Player): Boolean = player.vars["varbit.hosidius_evilgrapes"] >= 2

    fun purchaseRefusal(player: Player): String? = when {
        !unlocked(player) -> "You must unlock Bologa's blessings before buying them."
        player.basePrayerLvl < FarmingTitheData.row.blessingPrayer -> "You need a Prayer level of 50 to use Bologa's blessing."
        player.cookingLvl < FarmingTitheData.row.blessingCooking -> "You need a Cooking level of 65 to use Bologa's blessing."
        else -> null
    }
}
