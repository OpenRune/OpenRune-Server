package org.rsmod.content.skills.fishing

import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj

class TackleBoxDeathTest {
    @Test
    fun GameTestState.`the box keeps its contents with the setting off`() =
        runBoxTest(carryBox = true) {
            assertEquals(emptyList<InvObj>(), TackleBoxDeath().empty(player, isPvpDeath = false))
            assertEquals(BAIT_COUNT, box.count(BAIT))
        }

    @Test
    fun GameTestState.`the setting empties the box into the death`() =
        runBoxTest(carryBox = true) {
            player.setVarBit(SETTING, 1)

            assertEquals(listOf(InvObj(BAIT, BAIT_COUNT)), TackleBoxDeath().empty(player, false))
            assertEquals(0, box.count(BAIT))
        }

    @Test
    fun GameTestState.`a pvp death always empties the box`() =
        runBoxTest(carryBox = true) {
            assertEquals(listOf(InvObj(BAIT, BAIT_COUNT)), TackleBoxDeath().empty(player, true))
            assertEquals(0, box.count(BAIT))
        }

    @Test
    fun GameTestState.`nothing is taken without the box`() =
        runBoxTest(carryBox = false) {
            player.setVarBit(SETTING, 1)

            assertEquals(emptyList<InvObj>(), TackleBoxDeath().empty(player, true))
            assertEquals(BAIT_COUNT, box.count(BAIT))
        }

    private val GameTestScope.box
        get() = player.invMap.getOrPut("inv.tackle_box")

    private fun GameTestState.runBoxTest(carryBox: Boolean, body: GameTestScope.() -> Unit) =
        runGameTest {
            player.clearAllInvs()
            if (carryBox) {
                player.inv[0] = InvObj("obj.tackle_box", 1)
            }
            box[0] = InvObj(BAIT, BAIT_COUNT)
            body()
        }

    private companion object {
        const val SETTING = "varbit.emptyondeath_tacklebox"
        const val BAIT = "obj.fishing_bait"
        const val BAIT_COUNT = 50
    }
}
