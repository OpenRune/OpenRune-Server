package org.rsmod.content.areas.city.lumbridge.npcs

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.Shops
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.shop.Shop

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class GeneralStoreSaleTest {
    @Test
    fun GameTestState.`general store buys an item it does not stock`() =
        runInjectedGameTest(Shops::class, null, ShopScript::class) { shops ->
            val shop = openShop(shops, "inv.generalshop1")
            assertEquals(0, shop.inv.count(BRONZE_BAR))

            sellFirstSlot()

            assertEquals(0, player.inv.count(BRONZE_BAR))
            assertEquals(3, player.inv.count(COINS))
            assertEquals(1, shop.inv.count(BRONZE_BAR))
        }

    @Test
    fun GameTestState.`specialty shop still refuses an item it does not stock`() =
        runInjectedGameTest(Shops::class, null, ShopScript::class) { shops ->
            val shop = openShop(shops, "inv.axeshop")

            sellFirstSlot()

            assertEquals(1, player.inv.count(BRONZE_BAR))
            assertEquals(0, player.inv.count(COINS))
            assertEquals(0, shop.inv.count(BRONZE_BAR))
            assertMessageSent("You can't sell this item to this shop.")
        }

    private fun GameTestScope.openShop(shops: Shops, inv: String): Shop {
        player.clearInv()
        player.inv[0] = InvObj(BRONZE_BAR.asRSCM(RSCMType.OBJ), 1)
        shops.open(
            player = player,
            title = "",
            shopInv = inv,
            buyPercentage = 40.0,
            sellPercentage = 130.0,
            changePercentage = 3.0,
        )
        return checkNotNull(player.openedShop)
    }

    private fun GameTestScope.sellFirstSlot() {
        player.ifButton(
            ServerCacheManager.fromComponent("component.shopside:items"),
            comsub = 0,
            op = IfButtonOp.Op2,
            obj = BRONZE_BAR.asRSCM(RSCMType.OBJ),
        )
        advance()
    }

    private companion object {
        const val BRONZE_BAR = "obj.bronze_bar"
        const val COINS = "obj.coins"
    }
}
