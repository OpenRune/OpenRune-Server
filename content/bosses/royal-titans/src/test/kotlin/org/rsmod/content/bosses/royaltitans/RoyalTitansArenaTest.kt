package org.rsmod.content.bosses.royaltitans

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class RoyalTitansArenaTest {
    private val queenSpawn = CoordGrid(2906, 9566)
    private val kingSpawn = CoordGrid(2914, 9566)

    @Test
    fun `the phase two band is five wide and seven deep`() {
        val band = RoyalTitansArena.bandTiles(queenSpawn, TitanElement.Fire)
        assertEquals(35, band.size)
        assertEquals(2909, band.minOf { it.x })
        assertEquals(2913, band.maxOf { it.x })
        assertEquals(9564, band.minOf { it.z })
        assertEquals(9570, band.maxOf { it.z })
    }

    @Test
    fun `fire owns everything north of the safe tile and ice the rest`() {
        val safe = CoordGrid(2910, 9567)
        val fire = RoyalTitansArena.waveTiles(queenSpawn, TitanElement.Fire, safe, null)
        val ice = RoyalTitansArena.waveTiles(kingSpawn, TitanElement.Ice, safe, null)
        assertFalse(safe in fire)
        assertFalse(safe in ice)
        assertTrue(CoordGrid(2911, 9567) in fire)
        assertTrue(CoordGrid(2909, 9567) in ice)
        assertTrue(CoordGrid(2910, 9570) in fire)
        assertTrue(CoordGrid(2910, 9564) in ice)
        assertEquals(34, (fire + ice).size)
    }

    @Test
    fun `a lone titan blasts the whole band except the safe tile`() {
        val safe = CoordGrid(2909, 9570)
        val tiles =
            RoyalTitansArena.waveTiles(kingSpawn, TitanElement.Ice, safe, TitanElement.Ice)
        assertEquals(34, tiles.size)
        assertFalse(safe in tiles)
        assertTrue(CoordGrid(2910, 9570) in tiles)
        assertTrue(CoordGrid(2910, 9564) in tiles)
    }

    @Test
    fun `the safe tile stays in the central three rows unless survivor`() {
        val normal = RoyalTitansArena.safeTile(queenSpawn, TitanElement.Fire, null) { it.last }
        assertEquals(2913, normal.x)
        assertEquals(9568, normal.z)
        val queenSurvivor =
            RoyalTitansArena.safeTile(queenSpawn, TitanElement.Fire, TitanElement.Fire) { it.first }
        assertEquals(9564, queenSurvivor.z)
        val kingSurvivor =
            RoyalTitansArena.safeTile(queenSpawn, TitanElement.Fire, TitanElement.Ice) { it.first }
        assertEquals(9570, kingSurvivor.z)
    }

    @Test
    fun `elementals only spawn inside the five column corridor`() {
        val fireTiles =
            (0..20).flatMap {
                RoyalTitansArena.elementalSummonTiles(queenSpawn, TitanElement.Fire) { it.first }
            }
        val iceTiles =
            (0..20).flatMap {
                RoyalTitansArena.elementalSummonTiles(kingSpawn, TitanElement.Ice) { it.last }
            }
        assertTrue(fireTiles.all { it.x in 2909..2913 })
        assertTrue(iceTiles.all { it.x in 2909..2913 })
        assertTrue(fireTiles.all { it.z in 9561..9573 })
        assertTrue(iceTiles.all { it.z in 9561..9573 })
    }

    @Test
    fun `the phase two stomp reaches from each titan across the whole band`() {
        val fire = RoyalTitansArena.phaseTwoStompRows(queenSpawn, TitanElement.Fire)
        val ice = RoyalTitansArena.phaseTwoStompRows(kingSpawn, TitanElement.Ice)
        assertEquals(7, fire.size)
        assertEquals(7, ice.size)
        assertTrue(fire.all { it.size == 5 })
        assertEquals(9570, fire.first().first().z)
        assertEquals(9564, fire.last().first().z)
        assertEquals(9564, ice.first().first().z)
        assertEquals(9570, ice.last().first().z)
    }
}
