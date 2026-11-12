package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class PrifddinasPortalTest {
    private fun GameTestScope.climbWithPortal(portalIndex: Int): BoundLocInfo {
        setAgility(99)
        blockMarks()
        val portal = placeMapLoc(PORTAL_TILE, PORTAL, LocShape[10], LocAngle[3])
        startAt(START)
        advance()
        random.next = portalIndex
        cross(LADDER)
        return portal
    }

    private fun GameTestScope.travel(portal: BoundLocInfo): List<String> {
        player.opLoc1(portal)
        return advanceCollecting(ticks = 10)
    }

    @Test
    fun GameTestState.`the lap's portal opens after the obstacle before it`() =
        runAgilityTest(PrifddinasPortalScript::class) {
            climbWithPortal(portalIndex = 0)

            assertEquals(1, player.vars[SHOWN])
        }

    @Test
    fun GameTestState.`a portal skips ahead with a crystal shard and keeps the lap`() =
        runAgilityTest(PrifddinasPortalScript::class) {
            val portal = climbWithPortal(portalIndex = 0)
            val before = agilityXp()

            val messages = travel(portal)

            assertEquals(EXIT, player.coords)
            assertTrue(SHARD_MESSAGE in messages) { "$messages" }
            assertEquals(1, player.inv.count(SHARD))
            assertEquals(before + 820, agilityXp())
            assertEquals(0, player.vars[SHOWN])

            val lap = crossAll(REST_OF_LAP)
            assertTrue(lap.any { "Prifddinas Agility Course lap count is" in it }) { "$lap" }
        }

    @Test
    fun GameTestState.`a portal that is not shown does nothing`() =
        runAgilityTest(PrifddinasPortalScript::class) {
            val portal = climbWithPortal(portalIndex = 1)
            val start = player.coords

            travel(portal)

            assertEquals(start.level, player.coords.level)
            assertEquals(0, player.inv.count(SHARD))
            assertEquals(0, player.vars[SHOWN])
            assertEquals(2, player.vars[LAP_PORTAL])
        }

    @Test
    fun GameTestState.`a portal is used once`() =
        runAgilityTest(PrifddinasPortalScript::class) { deps ->
            val portal = climbWithPortal(portalIndex = 0)
            travel(portal)
            player.teleport(START.translate(4, 1, 2))
            advance()

            travel(portal)

            assertEquals(1, shardsOnFloor(deps) + player.inv.count(SHARD))
        }

    @Test
    fun GameTestState.`a full inventory drops the shard`() =
        runAgilityTest(PrifddinasPortalScript::class) { deps ->
            player.fillInv()
            val portal = climbWithPortal(portalIndex = 0)

            travel(portal)

            assertEquals(0, player.inv.count(SHARD))
            assertEquals(1, shardsOnFloor(deps))
        }

    private fun GameTestScope.shardsOnFloor(deps: AgilityTestDeps): Int {
        val shard = SHARD.asRSCM(RSCMType.OBJ)
        return deps.objRepo.findAll(EXIT).filter { it.type == shard }.sumOf { it.count }
    }

    private companion object {
        const val PORTAL = "loc.prif_agility_shortcut_portal_1"
        const val LADDER = "loc.prif_agility_start_ladder"
        const val SHARD = "obj.prif_crystal_shard"
        const val SHOWN = "varbit.prif_agility_shortcut_transmit"
        const val LAP_PORTAL = "varbit.agility_lap_portal"
        const val SHARD_MESSAGE = "You find a crystal shard on your way through the portal."
        val START = CoordGrid(3253, 6109, 0)
        val PORTAL_TILE = CoordGrid(3257, 6111, 2)
        val EXIT = CoordGrid(3275, 6105, 2)
        val REST_OF_LAP =
            listOf(
                "loc.prif_agility_chimney_jump",
                "loc.prif_agility_roof_jump",
                "loc.prif_agility_dark_hole_active",
                "loc.prif_agility_tree_ladder_long",
                "loc.prif_agility_rope_bridge1",
                "loc.prif_agility_tightrope1",
                "loc.prif_agility_rope_bridge2",
                "loc.prif_agility_tightrope2",
                "loc.prif_agility_tightrope3",
                "loc.prif_agility_dark_hole_end",
            )
    }
}
