package org.rsmod.content.bosses.giantmole

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.add
import org.rsmod.game.map.collision.set
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

class GiantMoleRulesTest {
    @Test
    fun `burrow window matches the ten hitpoint cutoff`() {
        assertFalse(GiantMoleRules.canBurrow(10, 200))
        assertTrue(GiantMoleRules.canBurrow(11, 200))
        assertTrue(GiantMoleRules.canBurrow(100, 200))
        assertFalse(GiantMoleRules.canBurrow(101, 200))
    }

    @Test
    fun `magic splashes cannot force a burrow`() {
        assertTrue(GiantMoleRules.isMagicSplash(HitType.Magic, 0))
        assertFalse(GiantMoleRules.isMagicSplash(HitType.Magic, 1))
        assertFalse(GiantMoleRules.isMagicSplash(HitType.Melee, 0))
    }

    @Test
    fun `size three melee range accounts for the footprint`() {
        val mole = CoordGrid(1759, 5189)
        assertTrue(GiantMoleRules.withinMeleeRange(mole, CoordGrid(1762, 5190)))
        assertFalse(GiantMoleRules.withinMeleeRange(mole, CoordGrid(1763, 5190)))
        assertEquals(0, GiantMoleRules.distance(mole, 3, CoordGrid(1761, 5191)))
    }

    @Test
    fun `only open light sources are extinguished`() {
        assertEquals("obj.unlit_candle", GiantMoleRules.openLightTarget("obj.lit_candle"))
        assertEquals(
            "obj.unlit_black_candle",
            GiantMoleRules.openLightTarget("obj.lit_black_candle"),
        )
        assertEquals("obj.torch_unlit", GiantMoleRules.openLightTarget("obj.torch_lit"))
        assertEquals("obj.oil_lamp_unlit", GiantMoleRules.openLightTarget("obj.oil_lamp_lit"))
        assertNull(GiantMoleRules.openLightTarget("obj.bullseye_lantern_lit"))
        assertNull(GiantMoleRules.openLightTarget("obj.candle_lantern_lit"))
        assertNull(GiantMoleRules.openLightTarget("obj.oil_lantern_lit"))
    }

    @Test
    fun `burrow points are inside the lair`() {
        assertTrue(GiantMoleRules.BURROW_POINTS.size >= 10)
        assertTrue(GiantMoleRules.BURROW_POINTS.all(GiantMoleRules::inMoleLair))
        assertTrue(GiantMoleRules.inMoleLair(GiantMoleRules.SPAWN))
    }

    @Test
    fun `burrow candidates require a clear size three footprint`() {
        val collision = walkableBurrowPoints()
        val current = GiantMoleRules.BURROW_POINTS.first()

        assertEquals(
            GiantMoleRules.BURROW_POINTS.size - 1,
            GiantMoleRules.burrowCandidates(collision, current).size,
        )

        collision.add(GiantMoleRules.BURROW_POINTS[1], CollisionFlag.LOC)
        assertTrue(
            GiantMoleRules.burrowCandidates(collision, current).none {
                it == GiantMoleRules.BURROW_POINTS[1]
            }
        )
    }

    @Test
    fun `burrow candidates avoid players nearby`() {
        val collision = walkableBurrowPoints()
        val current = GiantMoleRules.BURROW_POINTS.first()
        val blocked = listOf(GiantMoleRules.BURROW_POINTS[2])

        assertTrue(
            GiantMoleRules.burrowCandidates(collision, current, blocked).none {
                it == GiantMoleRules.BURROW_POINTS[2]
            }
        )
    }

    @Test
    fun `mole hill digs claim the hill and its adjacent tiles`() {
        assertTrue(GiantMoleRules.onMoleHill(CoordGrid(2987, 3387)))
        assertTrue(GiantMoleRules.onMoleHill(CoordGrid(2986, 3387)))
        assertFalse(GiantMoleRules.onMoleHill(CoordGrid(2987, 3390)))
    }

    private fun walkableBurrowPoints(): CollisionFlagMap {
        val collision = CollisionFlagMap()
        for (point in GiantMoleRules.BURROW_POINTS) {
            for (dx in 0 until GiantMoleRules.NPC_SIZE) {
                for (dz in 0 until GiantMoleRules.NPC_SIZE) {
                    collision[point.translate(dx, dz)] = 0
                }
            }
        }
        return collision
    }
}
