package org.rsmod.content.areas.city.alkharid.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
class CoinTradesTest {
    @Test
    fun GameTestState.`buying takes the price and gives one item`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, 10)
            var bought = false
            player.withProtectedAccess { bought = buyForCoins(KEBAB, 1) }
            assertTrue(bought)
            assertEquals(9, player.inv.count(COINS))
            assertEquals(1, player.inv.count(KEBAB))
        }

    @Test
    fun GameTestState.`a full pack keeps the coins`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, 10)
            fillAllBut(freeSlots = 0)
            var bought = true
            player.withProtectedAccess { bought = buyForCoins(KEBAB, 1) }
            assertFalse(bought)
            assertEquals(10, player.inv.count(COINS))
            assertEquals(0, player.inv.count(KEBAB))
        }

    @Test
    fun GameTestState.`paying the last coins frees their slot for the item`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, 1)
            fillAllBut(freeSlots = 0)
            var bought = false
            player.withProtectedAccess { bought = buyForCoins(KEBAB, 1) }
            assertTrue(bought)
            assertEquals(0, player.inv.count(COINS))
            assertEquals(1, player.inv.count(KEBAB))
        }

    @Test
    fun GameTestState.`too few coins buys nothing`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, 2)
            var bought = true
            player.withProtectedAccess { bought = buyForCoins(SILK, 3) }
            assertFalse(bought)
            assertEquals(2, player.inv.count(COINS))
            assertEquals(0, player.inv.count(SILK))
        }

    @Test
    fun GameTestState.`swapping gauntlets takes the old pair and the fee in one step`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, 25_000)
            player.inv[1] = obj(COOKING, 1)
            fillAllBut(freeSlots = 0)
            var swapped = false
            player.withProtectedAccess { swapped = swapForCoins(COOKING, GOLDSMITHING, 25_000) }
            assertTrue(swapped)
            assertEquals(0, player.inv.count(COINS))
            assertEquals(0, player.inv.count(COOKING))
            assertEquals(1, player.inv.count(GOLDSMITHING))
        }

    @Test
    fun GameTestState.`a short fee keeps the old gauntlets`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, 24_999)
            player.inv[1] = obj(COOKING, 1)
            var swapped = true
            player.withProtectedAccess { swapped = swapForCoins(COOKING, GOLDSMITHING, 25_000) }
            assertFalse(swapped)
            assertEquals(24_999, player.inv.count(COINS))
            assertEquals(1, player.inv.count(COOKING))
            assertEquals(0, player.inv.count(GOLDSMITHING))
        }

    private fun obj(name: String, count: Int) = InvObj(name.asRSCM(RSCMType.OBJ), count)

    private fun GameTestScope.fillAllBut(freeSlots: Int) {
        for (slot in 0 until player.inv.size) {
            if (player.inv[slot] == null) {
                player.inv[slot] = obj(FILLER, 1)
            }
        }
        var freed = 0
        for (slot in player.inv.size - 1 downTo 0) {
            if (freed == freeSlots) break
            if (player.inv[slot]?.id == FILLER.asRSCM(RSCMType.OBJ)) {
                player.inv[slot] = null
                freed++
            }
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val KEBAB = "obj.kebab"
        const val SILK = "obj.silk"
        const val COOKING = "obj.gauntlets_of_cooking"
        const val GOLDSMITHING = "obj.gauntlets_of_goldsmithing"
        const val FILLER = "obj.beer"
    }
}
