package org.rsmod.content.areas.misc.monastery.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class BrotherJeredSaleTest {
    @Test
    fun GameTestState.`buying the cape takes the price and gives cape and hood`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, PRAYER_CAPE_PRICE + 7)
            var bought = false
            player.withProtectedAccess { bought = buyPrayerCape() }
            assertTrue(bought)
            assertEquals(7, player.inv.count(COINS))
            assertEquals(1, player.inv.count(PRAYER_CAPE))
            assertEquals(1, player.inv.count(PRAYER_HOOD))
        }

    @Test
    fun GameTestState.`exact coins with a full pack keeps the coins rather than losing the hood`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, PRAYER_CAPE_PRICE)
            fillAllBut(freeSlots = 0)
            var bought = true
            player.withProtectedAccess { bought = buyPrayerCape() }
            assertFalse(bought)
            assertEquals(PRAYER_CAPE_PRICE, player.inv.count(COINS))
            assertEquals(0, player.inv.count(PRAYER_CAPE))
            assertEquals(0, player.inv.count(PRAYER_HOOD))
        }

    @Test
    fun GameTestState.`exact coins and one free slot fits cape and hood`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, PRAYER_CAPE_PRICE)
            fillAllBut(freeSlots = 1)
            var bought = false
            player.withProtectedAccess { bought = buyPrayerCape() }
            assertTrue(bought)
            assertEquals(0, player.inv.count(COINS))
            assertEquals(1, player.inv.count(PRAYER_CAPE))
            assertEquals(1, player.inv.count(PRAYER_HOOD))
        }

    @Test
    fun GameTestState.`one coin short buys nothing`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(COINS, PRAYER_CAPE_PRICE - 1)
            var bought = true
            player.withProtectedAccess { bought = buyPrayerCape() }
            assertFalse(bought)
            assertEquals(PRAYER_CAPE_PRICE - 1, player.inv.count(COINS))
            assertEquals(0, player.inv.count(PRAYER_CAPE))
        }

    @Test
    fun GameTestState.`blessing swaps exactly one symbol`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = obj(UNBLESSED_SYMBOL, 1)
            player.inv[1] = obj(UNBLESSED_SYMBOL, 1)
            var blessed = false
            player.withProtectedAccess { blessed = blessHolySymbol() }
            assertTrue(blessed)
            assertEquals(1, player.inv.count(UNBLESSED_SYMBOL))
            assertEquals(1, player.inv.count(BLESSED_SYMBOL))
        }

    @Test
    fun GameTestState.`blessing without a symbol gives nothing`() =
        runGameTest {
            player.clearInv()
            var blessed = true
            player.withProtectedAccess { blessed = blessHolySymbol() }
            assertFalse(blessed)
            assertEquals(0, player.inv.count(BLESSED_SYMBOL))
        }

    private fun obj(name: String, count: Int) = InvObj(name.asRSCM(RSCMType.OBJ), count)

    private fun GameTestScope.fillAllBut(freeSlots: Int) {
        for (slot in 1 until player.inv.size - freeSlots) {
            player.inv[slot] = obj(FILLER, 1)
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val FILLER = "obj.beer"
    }
}
