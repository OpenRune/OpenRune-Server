package org.rsmod.content.areas.misc.fossilunderwater

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.shops.ShopScript
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.entity.Npc
import org.rsmod.game.inv.InvObj
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class MairinTest {
    private fun GameTestState.runMairinTest(body: GameTestScope.() -> Unit) =
        runGameTest(MairinScript::class, ShopScript::class) {
            advance()
            player.observerUUID = player.uuid
            player.clearAllInvs()
            setLevel(AGILITY, 99)
            setLevel(THIEVING, 99)
            setVar(BOWL_ON_PILE, 0)
            body()
        }

    private fun GameTestScope.mairin(): Npc {
        player.teleport(STAND)
        val mairin = spawnNpc(MAIRIN_TILE, checkNotNull(ServerCacheManager.getNpc(MAIRIN.asRSCM())))
        advance()
        return mairin
    }

    private fun GameTestScope.buyFirstSlot() {
        player.ifButton(
            ServerCacheManager.fromComponent("component.shopmain:items"),
            comsub = 1,
            op = IfButtonOp.Op2,
            obj = SMALL_FOSSIL.asRSCM(RSCMType.OBJ),
        )
        advance()
    }

    private fun GameTestScope.exchange(button: String, op: IfButtonOp) {
        player.ifButton(ServerCacheManager.fromComponent(button), op = op)
        advance()
    }

    @Test
    fun GameTestState.`mairin sells a small fossil for 100 mermaid's tears`() = runMairinTest {
        player.inv[0] = InvObj(MERMAID_TEAR, 150)
        player.opNpc3(mairin())
        advance(3)
        assertNotNull(player.openedShop)

        buyFirstSlot()

        assertEquals(50, player.inv.count(MERMAID_TEAR))
        assertEquals(1, player.inv.count(SMALL_FOSSIL))
    }

    @Test
    fun GameTestState.`mairin refuses a player short of tears`() = runMairinTest {
        player.inv[0] = InvObj(MERMAID_TEAR, 99)
        player.opNpc3(mairin())
        advance(3)

        buyFirstSlot()

        assertEquals(99, player.inv.count(MERMAID_TEAR))
        assertEquals(0, player.inv.count(SMALL_FOSSIL))
    }

    @Test
    fun GameTestState.`a tear trains agility at mairin's rate`() = runMairinTest {
        player.inv[0] = InvObj(GLISTENING_TEAR, 3)
        player.opNpc4(mairin())
        advance(3)
        val agility = xp(AGILITY)
        val thieving = xp(THIEVING)

        exchange(TRAIN_AGILITY, IfButtonOp.Op1)

        assertEquals(2, player.inv.count(GLISTENING_TEAR))
        assertEquals(agility + 2646, xp(AGILITY))
        assertEquals(thieving, xp(THIEVING))
    }

    @Test
    fun GameTestState.`exchanging all pays for 700 tears and loses the rest`() = runMairinTest {
        player.inv[0] = InvObj(GLISTENING_TEAR, 800)
        player.opNpc4(mairin())
        advance(3)
        val agility = xp(AGILITY)
        val thieving = xp(THIEVING)

        exchange(TRAIN_BOTH, IfButtonOp.Op4)

        assertEquals(0, player.inv.count(GLISTENING_TEAR))
        assertEquals(agility + 1764 * 700, xp(AGILITY))
        assertEquals(thieving + 6468 * 700, xp(THIEVING))
    }

    @Test
    fun GameTestState.`no tears to exchange pays nothing`() = runMairinTest {
        player.opNpc4(mairin())
        advance(3)
        val thieving = xp(THIEVING)

        player.ifButton(ServerCacheManager.fromComponent(TRAIN_THIEVING), op = IfButtonOp.Op1)
        val messages = advanceCollecting(1)

        assertTrue("You don't have any glistening tears to exchange." in messages)
        assertEquals(thieving, xp(THIEVING))
    }

    @Test
    fun GameTestState.`a bowl left on the pile is taken back once`() = runMairinTest {
        player.teleport(PILE_STAND)
        player.inv[0] = InvObj(FISHBOWL)
        player.inv[1] = InvObj(FISHBOWL)
        val pile = place(PILE, PILE_TILE, angle = 1)
        advance()

        player.opLoc1(pile)
        player.opLoc1(pile)
        advance(4)
        assertEquals(1, player.inv.count(FISHBOWL))
        assertEquals(1, player.vars[BOWL_ON_PILE])

        player.opLoc3(pile)
        player.opLoc3(pile)
        advance(4)
        assertEquals(2, player.inv.count(FISHBOWL))
        assertEquals(0, player.vars[BOWL_ON_PILE])

        player.opLoc3(pile)
        advance(4)
        assertEquals(2, player.inv.count(FISHBOWL))
    }

    private companion object {
        const val MAIRIN = "npc.fossil_mermaid_deepwater"
        const val SMALL_FOSSIL = "obj.fossil_small_unid"
        const val FISHBOWL = "obj.hundred_pirate_diving_helmet"
        const val PILE = "loc.fossil_underwater_bowlhelm_pile"
        const val BOWL_ON_PILE = "varbit.fossil_underwater_bowl"
        const val TRAIN_AGILITY = "component.fossil_underwater_training:agility"
        const val TRAIN_THIEVING = "component.fossil_underwater_training:thieving"
        const val TRAIN_BOTH = "component.fossil_underwater_training:both"

        val STAND = CoordGrid(3222, 3218, 0)
        val MAIRIN_TILE = CoordGrid(3224, 3218, 0)
        val PILE_TILE = CoordGrid(3766, 10285, 1)
        val PILE_STAND = CoordGrid(3766, 10284, 1)
    }
}
