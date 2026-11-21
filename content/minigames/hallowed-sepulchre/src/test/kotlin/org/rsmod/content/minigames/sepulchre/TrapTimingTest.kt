package org.rsmod.content.minigames.sepulchre

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class TrapTimingTest {
    @Test
    fun `a floor 1 wizard statue repeats its captured eight tick cycle`() {
        val statue = (0 until 8).map { t -> seq(t, period = 8, phases = listOf(0)) }
        assertEquals(
            listOf(
                TrapTiming.FLAME_START,
                TrapTiming.FLAME_ATTACK,
                TrapTiming.FLAME_ATTACK,
                TrapTiming.FLAME_ATTACK,
                TrapTiming.FLAME_FINISH,
                TrapTiming.FLAME_IDLE,
                TrapTiming.FLAME_READY,
                TrapTiming.FLAME_READY,
            ),
            statue,
        )
        val fire =
            (0 until 8).map {
                TrapTiming.fireSeq(TrapTiming.sinceAttack(it, 8, listOf(0)), FLOOR_ATTACK)
            }
        assertEquals(
            listOf(
                TrapTiming.FIRE_READY,
                TrapTiming.FIRE_START,
                TrapTiming.FIRE_BLAST,
                TrapTiming.FIRE_BLAST,
                TrapTiming.FIRE_END,
                TrapTiming.FIRE_IDLE,
                TrapTiming.FIRE_IDLE,
                TrapTiming.FIRE_IDLE,
            ),
            fire,
        )
    }

    @Test
    fun `a flame burns from the tick after its start to the finish`() {
        val burning =
            (0 until 8).filter {
                TrapTiming.flameBurns(TrapTiming.sinceAttack(it, 8, listOf(0)), FLOOR_ATTACK)
            }
        assertEquals(listOf(1, 2, 3, 4), burning)
    }

    @Test
    fun `a sixteen tick statue waits idle between attacks and readies two ticks ahead`() {
        assertEquals(TrapTiming.FLAME_IDLE, seq(5, period = 16, phases = listOf(0)))
        assertEquals(TrapTiming.FLAME_IDLE, seq(13, period = 16, phases = listOf(0)))
        assertEquals(TrapTiming.FLAME_READY, seq(14, period = 16, phases = listOf(0)))
        assertEquals(TrapTiming.FLAME_START, seq(8, period = 16, phases = listOf(8)))
        assertEquals(TrapTiming.FLAME_START, seq(8, period = 16, phases = listOf(0, 8)))
        assertEquals(TrapTiming.FLAME_READY, seq(-1, period = 16, phases = listOf(0)))
    }

    @Test
    fun `the floor 1 north sword follows its captured path`() {
        val offsets = (1..16).map { TrapTiming.swordOffset(it, reach = 10, speed = 2, hold = 3) }
        assertEquals(
            listOf(2, 4, 6, 8, 10, 10, 10, 10, 8, 6, 4, 2, 0, -2, -2, null),
            offsets,
        )
        assertEquals(16, TrapTiming.swordLife(reach = 10, speed = 2, hold = 3))
    }

    @Test
    fun `the floor 1 south sword stops short on its last step out and back`() {
        val offsets = (1..26).map { TrapTiming.swordOffset(it, reach = 19, speed = 2, hold = 3) }
        assertEquals(19, offsets[9])
        assertEquals(17, offsets[13])
        assertEquals(-2, offsets[23])
        assertNull(offsets[25])
        assertEquals(26, TrapTiming.swordLife(reach = 19, speed = 2, hold = 3))
    }

    @Test
    fun `the floor 4 tier three sword reaches its mark in five throws`() {
        val offsets = (1..23).map { TrapTiming.swordOffset(it, reach = 17, speed = 4, hold = 10) }
        assertEquals(listOf(4, 8, 12, 16, 17), offsets.take(5))
        assertEquals(17, offsets[14])
        assertEquals(13, offsets[15])
        assertEquals(-4, offsets[20])
        assertNull(offsets[22])
    }

    @Test
    fun `a bolt is gone the tick after it reaches the end of its lane`() {
        val offsets = (1..6).map { TrapTiming.boltOffset(it, reach = 14, speed = 3) }
        assertEquals(listOf(3, 6, 9, 12, 14, null), offsets)
    }

    @Test
    fun `a fire loc lies along z unless it is turned east or west`() {
        val south = Fire(CoordGrid(2287, 5968, 2), angle = 0)
        val west = Fire(CoordGrid(2243, 5978, 2), angle = 3)
        assertEquals(listOf(5968, 5969, 5970), south.tiles.map { it.z })
        assertTrue(west.tiles.all { it.z == 5978 })
        assertEquals(listOf(2243, 2244, 2245), west.tiles.map { it.x })
        assertFalse(west.tiles.any { it.x == 2246 })
    }

    @Test
    fun `a floor 5 wizard statue breathes for two ticks on its fourteen tick cycle`() {
        val statue = (0 until 14).map { t -> seq(t, period = 14, phases = listOf(0), attack = 2) }
        assertEquals(TrapTiming.FLAME_START, statue[0])
        assertEquals(listOf(TrapTiming.FLAME_ATTACK, TrapTiming.FLAME_ATTACK), statue.subList(1, 3))
        assertEquals(TrapTiming.FLAME_FINISH, statue[3])
        assertTrue(statue.subList(4, 12).all { it == TrapTiming.FLAME_IDLE })
        assertEquals(listOf(TrapTiming.FLAME_READY, TrapTiming.FLAME_READY), statue.subList(12, 14))
        val fire = (0 until 5).map { TrapTiming.fireSeq(it, attack = 2) }
        assertEquals(
            listOf(
                TrapTiming.FIRE_READY,
                TrapTiming.FIRE_START,
                TrapTiming.FIRE_BLAST,
                TrapTiming.FIRE_END,
                TrapTiming.FIRE_IDLE,
            ),
            fire,
        )
        val burning = (0 until 14).filter { TrapTiming.flameBurns(it, attack = 2) }
        assertEquals(listOf(1, 2, 3), burning)
    }

    @Test
    fun `a floor 5 statue on both phases attacks every seven ticks`() {
        val starts =
            (0 until 28).filter {
                seq(it, period = 14, phases = listOf(0, 7), attack = 2) == TrapTiming.FLAME_START
            }
        assertEquals(listOf(0, 7, 14, 21), starts)
        assertEquals(TrapTiming.FLAME_START, seq(7, period = 14, phases = listOf(7), attack = 2))
        assertEquals(TrapTiming.FLAME_IDLE, seq(0, period = 14, phases = listOf(7), attack = 2))
    }

    @Test
    fun `the floor 5 swords are caught on their captured ticks`() {
        assertEquals(22, TrapTiming.swordLife(reach = 28, speed = 4, hold = 5))
        assertEquals(23, TrapTiming.swordLife(reach = 29, speed = 4, hold = 4))
        val upper = (1..22).map { TrapTiming.swordOffset(it, reach = 28, speed = 4, hold = 5) }
        assertEquals(listOf(4, 8, 12, 16, 20, 24, 28), upper.take(7))
        assertEquals(28, upper[11])
        assertEquals(24, upper[12])
        assertNull(upper[21])
    }

    @Test
    fun `lightning glows the tick before the start, strikes two after it and stuns twice`() {
        val period = 5
        val statue = (0 until period).map { TrapTiming.lightningStatueSeq(it) }
        assertEquals(
            listOf(
                TrapTiming.LIGHTNING_START,
                TrapTiming.LIGHTNING_IDLE,
                TrapTiming.LIGHTNING_ATTACK,
                TrapTiming.LIGHTNING_FINISH,
                TrapTiming.LIGHTNING_IDLE,
            ),
            statue,
        )
        val ground = (0 until period).map { TrapTiming.lightningGround(it, period) }
        assertEquals(
            listOf(null, null, TrapTiming.STRIKE, TrapTiming.STRIKE_FADE, TrapTiming.STRIKE_GLOW),
            ground,
        )
        assertEquals(TrapTiming.STRIKE_GLOW, TrapTiming.lightningGround(5, period = 6))
        assertNull(TrapTiming.lightningGround(4, period = 6))
        assertEquals(listOf(2, 3), (0 until 6).filter { TrapTiming.lightningStuns(it) })
    }

    @Test
    fun `floor 5 lightning strikes two crosses two tiles apart`() {
        val trap = lightning(listOf(CoordGrid(2280, 5876, 0), CoordGrid(2282, 5876, 0)))
        val tiles = trap.strikeTiles.map { it.x to it.z }.toSet()
        assertEquals(
            setOf(
                2280 to 5876,
                2282 to 5876,
                2279 to 5875,
                2281 to 5875,
                2283 to 5875,
                2279 to 5877,
                2281 to 5877,
                2283 to 5877,
            ),
            tiles,
        )
    }

    private fun lightning(strikes: List<CoordGrid>): Trap =
        Trap(
            floor = 5,
            kind = TrapKind.Lightning,
            statues = emptyList(),
            fires = emptyList(),
            period = 5,
            phases = listOf(0),
            launches = emptyList(),
            target = null,
            speed = 0,
            hold = 0,
            volley = 0,
            npc = null,
            thrown = null,
            attack = 0,
            strikes = strikes,
        )

    private fun seq(t: Int, period: Int, phases: List<Int>, attack: Int = FLOOR_ATTACK): String =
        TrapTiming.flameStatueSeq(
            TrapTiming.sinceAttack(t, period, phases),
            TrapTiming.untilAttack(t, period, phases),
            attack,
        )

    private companion object {
        const val FLOOR_ATTACK = 3
    }
}
