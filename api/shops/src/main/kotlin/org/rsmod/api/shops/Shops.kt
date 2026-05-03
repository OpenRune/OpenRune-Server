package org.rsmod.api.shops

import dev.openrune.ServerCacheManager
import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InvScope
import jakarta.inject.Inject
import org.rsmod.api.player.output.ClientScripts.interfaceInvInit
import org.rsmod.api.player.output.ClientScripts.shopMainInit
import org.rsmod.api.player.startInvTransmit
import org.rsmod.api.player.ui.ifOpenMainSidePair
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.shops.config.ShopParams
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop

public class Shops @Inject constructor(private val eventBus: EventBus) {
    public val globalInvs: MutableMap<String, Inventory> = mutableMapOf()

    public fun open(
        player: Player,
        activeNpc: Npc,
        title: String,
        shopInv: String,
        currency: String = "currency.standard_gp",
        subtext: String = DEFAULT_SUBTEXT,
    ) {
        val buyPercentage = activeNpc.type.param(ShopParams.shop_buy_percentage) / 10.0
        val sellPercentage = activeNpc.type.param(ShopParams.shop_sell_percentage) / 10.0
        val changePercentage = activeNpc.type.param(ShopParams.shop_change_percentage) / 10.0
        open(
            player = player,
            title = title,
            shopInv = shopInv,
            buyPercentage = buyPercentage,
            sellPercentage = sellPercentage,
            changePercentage = changePercentage,
            currency = currency,
            subtext = subtext,
        )
    }

    public fun open(
        player: Player,
        title: String,
        shopInv: String,
        buyPercentage: Double,
        sellPercentage: Double,
        changePercentage: Double,
        currency: String = "currency.standard_gp",
        subtext: String = DEFAULT_SUBTEXT,
    ) {
        val inv = shopInv.toInventory(player)
        open(
            player = player,
            title = title,
            shopInv = inv,
            sideInv = player.inv,
            currency = currency,
            buyPercentage = buyPercentage,
            sellPercentage = sellPercentage,
            changePercentage = changePercentage,
            subtext = subtext,
        )
    }

    public fun open(
        player: Player,
        title: String,
        shopInv: Inventory,
        sideInv: Inventory,
        currency: String = "currency.standard_gp",
        buyPercentage: Double,
        sellPercentage: Double,
        changePercentage: Double,
        subtext: String,
    ) {
        player.openedShop = Shop(shopInv, currency, buyPercentage, sellPercentage, changePercentage)

        player.startInvTransmit(shopInv)
        player.startInvTransmit(sideInv)
        player.ifOpenMainSidePair(
            main = "interface.shopmain",
            side = "interface.shopside",
            colour = -1,
            transparency = -1,
            eventBus = eventBus,
        )
        shopMainInit(player, shopInv.type, title)

        player.ifSetEvents(
            "component.shopmain:items",
            1..shopInv.size,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op10,
        )

        interfaceInvInit(
            player = player,
            inv = sideInv,
            target = "component.shopside:items",
            objRowCount = 4,
            objColCount = 7,
            op1 = "Value<col=ff9040>",
            op2 = "Sell 1<col=ff9040>",
            op3 = "Sell 5<col=ff9040>",
            op4 = "Sell 10<col=ff9040>",
            op5 = "Sell 50<col=ff9040>",
        )
        player.ifSetEvents(
            "component.shopside:items",
            0 until sideInv.size,
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op10,
        )
    }

    private fun String.toInventory(observer: Player): Inventory {
        val unpacked = ServerCacheManager.getInventory(this.asRSCM(RSCMType.INV)) ?: error("Error getting inv")
        return if (unpacked.scope == InvScope.Shared) {
            sharedInv()
        } else {
            privateInv(observer)
        }
    }

    private fun String.sharedInv(): Inventory =
        globalInvs.getOrPut(this) { createSharedInv() }

    private fun String.createSharedInv(): Inventory {
        val unpacked = ServerCacheManager.getInventory(this.asRSCM(RSCMType.INV)) ?: error("Error getting inv")
        check(unpacked.scope == InvScope.Shared) {
            "`shopInv` must have shared scope. (shopInv=$unpacked)"
        }
        return Inventory.create(RSCM.getReverseMapping(RSCMType.INV, unpacked.id))
    }

    private fun String.privateInv(player: Player): Inventory {
        val unpacked = ServerCacheManager.getInventory(this.asRSCM(RSCMType.INV)) ?: error("Error getting inv")
        check(unpacked.scope != InvScope.Shared) {
            "`shopInv` must not have shared scope. (shopInv=$unpacked)"
        }
        return player.invMap.getOrPut(RSCM.getReverseMapping(RSCMType.INV, unpacked.id))
    }

    public companion object {
        public const val DEFAULT_SUBTEXT: String =
            "Right click on shop to buy item - Right-click on inventory to sell item"
    }
}
