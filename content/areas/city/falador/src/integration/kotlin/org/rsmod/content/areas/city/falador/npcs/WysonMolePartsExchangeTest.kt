package org.rsmod.content.areas.city.falador.npcs

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.random.GameRandom
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class WysonMolePartsExchangeTest {
    @Test
    fun GameTestState.`exchanging parts keeps every roll when there is space`() =
        runInjectedGameTest(Deps::class, null) { deps ->
            player.clearAllInvs()
            player.inv[0] = obj("obj.mole_claw", 1)
            player.inv[1] = obj("obj.cert_mole_skin", 1)
            player.inv[2] = obj("obj.immaculate_mole_skin", 1)

            WysonMoleParts.exchange(player, deps.random, deps.objs)

            assertEquals(0, player.inv.count(PART_CLAW))
            assertEquals(0, player.inv.count(PART_SKIN))
            assertEquals(0, player.inv.count(PART_IMMACULATE))
            assertEquals(7, nestCount())
            assertEquals(0, boxCount())
        }

    @Test
    fun GameTestState.`extracting a nest box returns every nest it holds`() =
        runInjectedGameTest(Deps::class, null) {
            player.clearAllInvs()
            player.inv[0] = WysonNest.SEEDS.createBox(3)

            repeat(3) { assertTrue(WysonMoleParts.extract(player, 0, WysonNest.SEEDS)) }

            assertEquals(0, player.inv.count(WysonNest.SEEDS.box))
            assertEquals(3, player.inv.count(WysonNest.SEEDS.nest))
        }

    @Test
    fun GameTestState.`extracting a box retrieves one nest at a time`() =
        runInjectedGameTest(Deps::class, null) {
            player.clearAllInvs()
            player.inv[0] = WysonNest.SEEDS.createBox(5)

            assertTrue(WysonMoleParts.extract(player, 0, WysonNest.SEEDS))

            assertEquals(1, player.inv.count(WysonNest.SEEDS.nest))
            assertEquals(1, player.inv.count(WysonNest.SEEDS.box))
            assertEquals(4, storedBoxNests(WysonNest.SEEDS))
        }

    @Test
    fun GameTestState.`a full inventory can still take the last nest`() =
        runInjectedGameTest(Deps::class, null) {
            player.fillInv()
            player.inv[0] = WysonNest.SEEDS.createBox(1)

            assertTrue(WysonMoleParts.extract(player, 0, WysonNest.SEEDS))

            assertEquals(0, player.inv.count(WysonNest.SEEDS.box))
            assertEquals(1, player.inv.count(WysonNest.SEEDS.nest))
        }

    @Test
    fun GameTestState.`a box with one nest left can be emptied into a full inventory`() =
        runInjectedGameTest(Deps::class, null) {
            player.fillInv()
            player.inv[0] = WysonNest.SEEDS.createBox(2)
            player.inv[1] = null

            assertTrue(WysonMoleParts.extract(player, 0, WysonNest.SEEDS))
            assertEquals(1, player.inv.count(WysonNest.SEEDS.nest))
            assertEquals(1, storedBoxNests(WysonNest.SEEDS))

            assertTrue(WysonMoleParts.extract(player, 0, WysonNest.SEEDS))
            assertEquals(2, player.inv.count(WysonNest.SEEDS.nest))
            assertEquals(0, player.inv.count(WysonNest.SEEDS.box))
        }

    @Test
    fun GameTestState.`a full inventory cannot take a nest from a larger box`() =
        runInjectedGameTest(Deps::class, null) {
            player.fillInv()
            player.inv[0] = WysonNest.SEEDS.createBox(2)

            assertFalse(WysonMoleParts.extract(player, 0, WysonNest.SEEDS))

            assertEquals(1, player.inv.count(WysonNest.SEEDS.box))
            assertEquals(2, storedBoxNests(WysonNest.SEEDS))
            assertEquals(0, player.inv.count(WysonNest.SEEDS.nest))
        }

    @Test
    fun GameTestState.`a full inventory receives boxed nests without losing parts`() =
        runInjectedGameTest(Deps::class, null) { deps ->
            player.fillInv()
            player.inv[0] = obj("obj.mole_claw", 1)
            player.inv[1] = obj("obj.cert_mole_skin", 1)
            player.inv[2] = obj("obj.immaculate_mole_skin", 1)
            val before = player.inv.count(PART_CLAW) +
                player.inv.count(PART_SKIN) +
                player.inv.count(PART_IMMACULATE)

            WysonMoleParts.exchange(player, deps.random, deps.objs)

            assertEquals(before, 3)
            assertEquals(0, player.inv.count(PART_CLAW))
            assertEquals(0, player.inv.count(PART_SKIN))
            assertEquals(0, player.inv.count(PART_IMMACULATE))
            assertEquals(7, boxedNests())
            assertEquals(0, nestCount())
            assertTrue(boxCount() in 1..3)
            for (slot in player.inv.indices) {
                val obj = player.inv[slot] ?: continue
                if (WysonNest.entries.none { it.boxId == obj.id }) continue
                assertEquals(1, obj.count)
                assertTrue(obj.vars > 0)
            }
        }

    private fun GameTestScope.obj(name: String, count: Int): InvObj =
        InvObj(name.asRSCM(RSCMType.OBJ), count)

    private fun GameTestScope.nestCount(): Int =
        WysonNest.entries.sumOf { player.inv.count(it.nest) }

    private fun GameTestScope.boxCount(): Int =
        WysonNest.entries.sumOf { player.inv.count(it.box) }

    private fun GameTestScope.boxedNests(): Int =
        WysonNest.entries.sumOf { storedBoxNests(it) }

    private fun GameTestScope.storedBoxNests(nest: WysonNest): Int =
        player.inv.indices.sumOf { slot ->
            val obj = player.inv[slot]
            if (obj?.id == nest.boxId) obj.wysonNestCount() else 0
        }

    class Deps @Inject constructor(val objs: ObjRepository, val random: GameRandom)

    private companion object {
        const val PART_CLAW = "obj.mole_claw"
        const val PART_SKIN = "obj.cert_mole_skin"
        const val PART_IMMACULATE = "obj.immaculate_mole_skin"
    }
}
