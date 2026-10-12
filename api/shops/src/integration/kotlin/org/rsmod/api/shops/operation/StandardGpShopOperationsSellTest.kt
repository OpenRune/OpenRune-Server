package org.rsmod.api.shops.operation

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.shops.Shops
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.shop.Shop

/* Obj transaction system is not thread-safe. */
@Execution(ExecutionMode.SAME_THREAD)
class StandardGpShopOperationsSellTest {
    @Test
    fun GameTestState.`selling five conserves every obj and pays the falling price`() =
        runGameTest(ShopScript::class) {
            // given
            val shop = openShopStocking(SOLD_OBJ)
            player.inv[0] = InvObj(SOLD_OBJ, RUNE_STACK)
            player.inv[1] = InvObj(UNTOUCHED_OBJ)
            val expected =
                holdings(shop) +
                    mapOf(
                        "inv $SOLD_OBJ" to RUNE_STACK - 5L,
                        "shop $SOLD_OBJ" to 5L,
                        "inv $COINS" to PAYMENT_FOR_FIVE,
                    )

            // when
            clickSell(slot = 0, OP_SELL5)
            advance()

            // then
            assertEquals(expected, holdings(shop))
        }

    @Test
    fun GameTestState.`selling into a full inventory with no room for coins rolls back`() =
        runGameTest(ShopScript::class) {
            // given
            val shop = openShopStocking(SOLD_OBJ)
            player.fillInv()
            player.inv[0] = InvObj(SOLD_OBJ, RUNE_STACK)
            val before = holdings(shop)

            // when
            clickSell(slot = 0, OP_SELL1)
            advance()

            // then
            assertEquals(before, holdings(shop))
        }

    @Test
    fun GameTestState.`selling with a max coin stack sells nothing and keeps the stack at max`() =
        runGameTest(ShopScript::class) {
            // given
            val shop = openShopStocking(SOLD_OBJ)
            player.inv[0] = InvObj(SOLD_OBJ, RUNE_STACK)
            player.inv[1] = InvObj(COINS, Int.MAX_VALUE)
            val before = holdings(shop)

            // when
            clickSell(slot = 0, OP_SELL5)
            advance()

            // then
            assertEquals(before, holdings(shop))
        }

    @Test
    fun GameTestState.`two sell clicks in one tick for a single obj sell it once`() =
        runGameTest(ShopScript::class) {
            // given
            val shop = openShopStocking(SOLD_OBJ)
            player.inv[0] = InvObj(SOLD_OBJ, 1)
            val expected =
                holdings(shop) - "inv $SOLD_OBJ" +
                    mapOf("shop $SOLD_OBJ" to 1L, "inv $COINS" to PAYMENT_FOR_ONE)

            // when
            clickSell(slot = 0, OP_SELL1)
            clickSell(slot = 0, OP_SELL1)
            advance()

            // then
            assertEquals(expected, holdings(shop))
        }

    @Test
    fun GameTestState.`a sell click for an obj that left the inventory pays nothing`() =
        runGameTest(ShopScript::class) {
            // given
            val shop = openShopStocking(SOLD_OBJ)
            player.inv[0] = InvObj(SOLD_OBJ, RUNE_STACK)
            clickSell(slot = 0, OP_SELL5)
            player.inv[0] = null
            val before = holdings(shop)

            // when
            advance()

            // then
            assertEquals(before, holdings(shop))
        }

    private fun GameTestScope.openShopStocking(obj: String): Shop {
        val inventory = Inventory.create(SHOP_INV)
        inventory.fillNulls()
        inventory[0] = InvObj(obj, 0)
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
            subtext = "",
        )
        client.clearOutgoing()
        check(player.ui.containsModal("interface.shopside"))
        return checkNotNull(player.openedShop)
    }

    private fun GameTestScope.clickSell(slot: Int, op: IfButtonOp) {
        val sideItems = ServerCacheManager.fromComponent("component.shopside:items")
        player.ifButton(sideItems, comsub = slot, obj = player.inv[slot]?.id, op = op)
    }

    private fun GameTestScope.holdings(shop: Shop): Map<String, Long> =
        holdings("inv", player.inv) + holdings("shop", shop.inv)

    private fun holdings(owner: String, inv: Inventory): Map<String, Long> =
        inv.objs
            .filterNotNull()
            .filter { it.count > 0 }
            .groupBy { RSCM.getReverseMapping(RSCMType.OBJ, it.id) }
            .mapKeys { (obj, _) -> "$owner $obj" }
            .mapValues { (_, objs) -> objs.sumOf { it.count.toLong() } }

    private companion object {
        const val SHOP_INV = "inv.generalshop1"
        const val SOLD_OBJ = "obj.deathrune"
        const val UNTOUCHED_OBJ = "obj.rune_axe"
        const val COINS = "obj.coins"
        const val RUNE_STACK = 100
        const val PAYMENT_FOR_ONE = 72L
        const val PAYMENT_FOR_FIVE = 304L
        val OP_SELL1 = IfButtonOp.Op2
        val OP_SELL5 = IfButtonOp.Op3
    }
}
