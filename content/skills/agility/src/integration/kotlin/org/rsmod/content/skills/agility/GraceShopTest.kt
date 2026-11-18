package org.rsmod.content.skills.agility

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

class GraceShopTest {
    private fun GameTestScope.openGraceShop(marks: Int) {
        player.teleport(STAND)
        player.inv[0] = InvObj(MARK, marks)
        val grace = spawnNpc(GRACE_TILE, checkNotNull(ServerCacheManager.getNpc(GRACE.asRSCM())))
        advance()
        player.opNpc3(grace)
        advance(ticks = 3)
        assertNotNull(player.openedShop)
    }

    private fun GameTestScope.clickBuy() {
        player.ifButton(
            ServerCacheManager.fromComponent("component.shopmain:items"),
            comsub = 1,
            op = IfButtonOp.Op2,
            obj = HOOD.asRSCM(RSCMType.OBJ),
        )
    }

    private fun GameTestScope.buyFirstSlot() {
        clickBuy()
        advance()
    }

    private fun GameTestScope.sellHood() {
        val slot = player.inv.objs.indexOfFirst { it?.id == HOOD.asRSCM(RSCMType.OBJ) }
        player.ifButton(
            ServerCacheManager.fromComponent("component.shopside:items"),
            comsub = slot,
            op = IfButtonOp.Op2,
            obj = HOOD.asRSCM(RSCMType.OBJ),
        )
        advance()
    }

    @Test
    fun GameTestState.`grace sells a graceful hood for 35 marks and buys it back for 28`() =
        runAgilityTest(GraceScript::class, ShopScript::class) {
            openGraceShop(marks = 100)

            buyFirstSlot()
            assertEquals(65, player.inv.count(MARK))
            assertEquals(1, player.inv.count(HOOD))

            sellHood()
            assertEquals(93, player.inv.count(MARK))
            assertEquals(0, player.inv.count(HOOD))
        }

    @Test
    fun GameTestState.`grace refuses a player short of marks`() =
        runAgilityTest(GraceScript::class, ShopScript::class) {
            openGraceShop(marks = 34)

            buyFirstSlot()

            assertEquals(34, player.inv.count(MARK))
            assertEquals(0, player.inv.count(HOOD))
        }

    @Test
    fun GameTestState.`a full pack keeps its marks and buys nothing`() =
        runAgilityTest(GraceScript::class, ShopScript::class) {
            openGraceShop(marks = 100)
            for (slot in 1 until player.inv.size) {
                player.inv[slot] = InvObj(BEER, 1)
            }

            buyFirstSlot()

            assertEquals(100, player.inv.count(MARK))
            assertEquals(0, player.inv.count(HOOD))
            assertEquals(player.inv.size - 1, player.inv.count(BEER))
        }

    @Test
    fun GameTestState.`two buys in one tick spend only the marks carried`() =
        runAgilityTest(GraceScript::class, ShopScript::class) {
            openGraceShop(marks = 50)

            clickBuy()
            clickBuy()
            advance()

            assertEquals(1, player.inv.count(HOOD))
            assertEquals(15, player.inv.count(MARK))
        }

    @Test
    fun GameTestState.`a sale onto a full stack of marks keeps the hood`() =
        runAgilityTest(GraceScript::class, ShopScript::class) {
            openGraceShop(marks = Int.MAX_VALUE)
            player.inv[1] = InvObj(HOOD, 1)

            sellHood()

            assertEquals(Int.MAX_VALUE, player.inv.count(MARK))
            assertEquals(1, player.inv.count(HOOD))
        }

    private companion object {
        const val GRACE = "npc.rooftops_grace"
        const val MARK = "obj.grace"
        const val HOOD = "obj.graceful_hood"
        const val BEER = "obj.beer"
        val STAND = CoordGrid(3222, 3218, 0)
        val GRACE_TILE = CoordGrid(3224, 3218, 0)
    }
}
