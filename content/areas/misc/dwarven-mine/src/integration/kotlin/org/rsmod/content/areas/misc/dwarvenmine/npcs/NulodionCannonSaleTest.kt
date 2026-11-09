package org.rsmod.content.areas.misc.dwarvenmine.npcs

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
class NulodionCannonSaleTest {
    @Test
    fun GameTestState.`buying takes exactly the price and gives the whole set once`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = InvObj(COINS.asRSCM(RSCMType.OBJ), CANNON_PRICE + 25)
            var bought = false
            player.withProtectedAccess { bought = buyCannonSet() }
            assertTrue(bought)
            assertEquals(25, player.inv.count(COINS))
            for (part in CANNON_SET) {
                assertEquals(1, player.inv.count(part), part)
            }
        }

    @Test
    fun GameTestState.`paying the exact price frees the coin slot for a part`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = InvObj(COINS.asRSCM(RSCMType.OBJ), CANNON_PRICE)
            fillAllBut(freeSlots = CANNON_SET.size - 1)
            var bought = false
            player.withProtectedAccess { bought = buyCannonSet() }
            assertTrue(bought)
            assertEquals(0, player.inv.count(COINS))
            for (part in CANNON_SET) {
                assertEquals(1, player.inv.count(part), part)
            }
        }

    @Test
    fun GameTestState.`without room for the whole set nothing changes hands`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = InvObj(COINS.asRSCM(RSCMType.OBJ), CANNON_PRICE + 25)
            fillAllBut(freeSlots = CANNON_SET.size - 1)
            var bought = true
            player.withProtectedAccess { bought = buyCannonSet() }
            assertFalse(bought)
            assertEquals(CANNON_PRICE + 25, player.inv.count(COINS))
            for (part in CANNON_SET) {
                assertEquals(0, player.inv.count(part), part)
            }
        }

    @Test
    fun GameTestState.`one coin short leaves the coins and gives nothing`() =
        runGameTest {
            player.clearInv()
            player.inv[0] = InvObj(COINS.asRSCM(RSCMType.OBJ), CANNON_PRICE - 1)
            var bought = true
            player.withProtectedAccess { bought = buyCannonSet() }
            assertFalse(bought)
            assertEquals(CANNON_PRICE - 1, player.inv.count(COINS))
            for (part in CANNON_SET) {
                assertEquals(0, player.inv.count(part), part)
            }
        }

    private fun GameTestScope.fillAllBut(freeSlots: Int) {
        val filler = InvObj(FILLER.asRSCM(RSCMType.OBJ), 1)
        for (slot in 1 until player.inv.size - freeSlots) {
            player.inv[slot] = filler
        }
    }

    private companion object {
        const val COINS = "obj.coins"
        const val FILLER = "obj.beer"
    }
}
