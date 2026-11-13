package org.rsmod.api.shops.operation

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.InvScope
import dev.openrune.types.InvStackType
import dev.openrune.types.InventoryServerType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.junit.jupiter.api.Test
import org.rsmod.api.config.Constants
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.Shops
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop

class StandardGpShopOperationsBuyTest {
    @Test
    fun GameTestState.`buy single obj successfully`() =
        runGameTest(ShopScript::class) {
            val shop = openGeneralShop()

            player.inv[0] = InvObj("obj.coins", 5)
            shop.inv[0] = InvObj("obj.newcomer_map", 5)

            buyStock(shop, "obj.newcomer_map", OP_BUY1)

            assertEquals(4, shop.inv.count("obj.newcomer_map"))
            assertEquals(1, player.inv.count("obj.newcomer_map"))
            assertEquals(4, player.inv.count("obj.coins"))
            assertNoMessageSent()
        }

    @Test
    fun GameTestState.`buy obj fails with no currency`() =
        runGameTest(ShopScript::class) {
            val shop = openGeneralShop()
            shop.inv[0] = InvObj("obj.newcomer_map", 5)
            buyStock(shop, "obj.newcomer_map", OP_BUY1)
            assertMessageSent("You don't have enough coins.")
        }

    @Test
    fun GameTestState.`buy obj fails with no currency or stock available`() =
        runGameTest(ShopScript::class) {
            val shop = openGeneralShop()
            shop.inv[0] = InvObj("obj.newcomer_map", 0)
            buyStock(shop, "obj.newcomer_map", OP_BUY1)
            assertMessageSent("That item is currently out of stock.")
        }

    @Test
    fun GameTestState.`buy 5 objs with limited inventory space`() =
        runGameTest(ShopScript::class) {
            val shop = openGeneralShop()

            player.fillInv()
            player.inv[0] = null
            player.inv[1] = InvObj("obj.coins", 6)
            shop.inv[0] = InvObj("obj.newcomer_map", 5)

            buyStock(shop, "obj.newcomer_map", OP_BUY5)

            assertEquals(1, player.inv.count("obj.newcomer_map"))
            assertEquals(4, shop.inv.count("obj.newcomer_map"))
            assertMessageSent("You don't have enough inventory space.")
        }

    @Test
    fun GameTestState.`buy 50 objs with sufficient space and currency`() =
        runGameTest(ShopScript::class) {
            val shop = openGeneralShop()

            player.fillInv()
            player.inv[0] = null
            player.inv[1] = null
            player.inv[2] = null
            player.inv[3] = null
            player.inv[4] = null
            player.inv[5] = InvObj("obj.coins", 5)
            shop.inv[0] = InvObj("obj.newcomer_map", 5)

            buyStock(shop, "obj.newcomer_map", OP_BUY50)

            assertEquals(0, shop.inv.count("obj.newcomer_map"))
            assertEquals(5, player.inv.count("obj.newcomer_map"))
            assertNoMessageSent()
        }

    @Test
    fun GameTestState.`buy 50 objs with no space and insufficient currency`() =
        runGameTest(ShopScript::class) {
            val shop = openGeneralShop()

            player.fillInv()
            player.inv[5] = InvObj("obj.coins", 2)
            shop.inv[0] = InvObj("obj.sos_security_book", 5)

            buyStock(shop, "obj.sos_security_book", OP_BUY50)

            assertEquals(4, shop.inv.count("obj.sos_security_book"))
            assertEquals(1, player.inv.count("obj.sos_security_book"))
            assertMessageSent("You don't have enough coins.")
        }

    @Test
    fun GameTestState.`buy all available stock successfully`() =
        runGameTest(ShopScript::class) {
            val shop = openGeneralShop()

            player.inv[0] = InvObj("obj.coins", Int.MAX_VALUE)
            shop.inv[0] = InvObj("obj.newcomer_map", 5)

            buyStock(shop, "obj.newcomer_map", OP_BUY50)

            assertEquals(0, shop.inv.count("obj.newcomer_map"))
            assertEquals(5, player.inv.count("obj.newcomer_map"))
            assertNoMessageSent()
        }

    private fun GameTestScope.buyStock(shop: Shop, obj: String, op: IfButtonOp) {
        val slot = shop.inv.indexOfFirst { it?.id == obj.asRSCM(RSCMType.OBJ) }
        check(slot != -1) { "Obj not found in stock: obj=$obj, stock=${shop.inv}" }

        val obj = shop.inv.getValue(slot)
        player.ifButton(ServerCacheManager.fromComponent("component.shopmain:items"), comsub = slot + 1, obj = obj.id, op = op)
        advance(ticks = 1)
    }

    private fun GameTestScope.openGeneralShop(): Shop {
        val inventory = createShopInv()
        val shops = Shops(eventBus)

        shops.open(
            player = player,
            title = "",
            shopInv = inventory,
            sideInv = player.inv,
            currency = "currency.standard_gp",
            buyPercentage = 40.0,
            sellPercentage = 130.0,
            changePercentage = 3.0,
        )

        // Clear out the player's outgoing messages from opening shop.
        client.clearOutgoing()

        check(player.ui.containsModal("interface.shopmain"))
        check(player.ui.containsModal("interface.shopside"))
        val shop = checkNotNull(player.openedShop)
        return shop
    }

    private fun createShopInv(): Inventory {
        val type =
            InventoryServerType(
                id = "inv.generalshop1".asRSCM(RSCMType.INV),
                scope = InvScope.Shared,
                stack = InvStackType.Always,
                size = Constants.shop_default_size,
                flags = InventoryServerType.PROTECT_FLAG or InventoryServerType.RESTOCK_FLAG or InventoryServerType.ALL_STOCK_FLAG,
            )
        return Inventory(type, arrayOfNulls(type.size))
    }

    private companion object {
        private val OP_BUY1 = IfButtonOp.Op2
        private val OP_BUY5 = IfButtonOp.Op3
        private val OP_BUY50 = IfButtonOp.Op5
    }
}
