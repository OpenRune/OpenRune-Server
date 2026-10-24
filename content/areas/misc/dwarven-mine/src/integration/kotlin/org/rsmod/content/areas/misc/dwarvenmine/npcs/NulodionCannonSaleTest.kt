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

    @Test
    fun GameTestState.`a lost cannon is replaced with its four parts and keeps its balls`() =
        runGameTest {
            player.clearInv()
            player.setVarp("varp.dropcannon", 4)
            player.setVarp("varp.rockthrower", 7)
            player.setVarBit("varbit.mcannon_decayed", 1)
            var given = false
            player.withProtectedAccess { given = replaceLostCannon() }
            assertTrue(given)
            for (part in CANNON_SET.take(4)) {
                assertEquals(1, player.inv.count(part), part)
            }
            assertEquals(0, player.vars["varp.dropcannon"])
            assertEquals(0, player.vars["varbit.mcannon_decayed"])
            assertEquals(7, player.vars["varp.rockthrower"])
        }

    @Test
    fun GameTestState.`a partly assembled cannon is replaced with only the deployed parts`() =
        runGameTest {
            for (stage in 1..3) {
                player.clearInv()
                player.setVarp("varp.dropcannon", stage)
                player.setVarBit("varbit.mcannon_decayed", 1)
                var given = false
                player.withProtectedAccess { given = replaceLostCannon() }
                assertTrue(given)
                CANNON_SET.take(4).forEachIndexed { index, part ->
                    val expected = if (index < stage) 1 else 0
                    assertEquals(expected, player.inv.count(part), "$stage $part")
                }
                assertEquals(0, player.vars["varp.dropcannon"])
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
