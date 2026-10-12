package org.rsmod.content.bosses.brutus

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class BrutusStompTest {
    private val sw = CoordGrid(3260, 3286)

    private fun tiles(vararg offsets: Pair<Int, Int>): Set<CoordGrid> =
        offsets.map { (x, z) -> sw.translate(x, z) }.toSet()

    private fun stomp(x: Int, z: Int): Set<CoordGrid> =
        Brutus.stompTiles(sw, sw.translate(x, z)).toSet()

    @Test
    fun `a player off the middle of a side gets the row along that side`() {
        assertEquals(tiles(0 to -1, 1 to -1, 2 to -1), stomp(1, -1))
        assertEquals(tiles(0 to -1, 1 to -1, 2 to -1), stomp(1, -3))
        assertEquals(tiles(0 to 3, 1 to 3, 2 to 3), stomp(1, 4))
        assertEquals(tiles(-1 to 0, -1 to 1, -1 to 2), stomp(-2, 1))
        assertEquals(tiles(3 to 0, 3 to 1, 3 to 2), stomp(3, 1))
    }

    @Test
    fun `a player off a corner gets an L of five round that corner`() {
        assertEquals(tiles(-1 to -1, 0 to -1, 1 to -1, -1 to 0, -1 to 1), stomp(-1, -1))
        assertEquals(tiles(3 to 3, 2 to 3, 1 to 3, 3 to 2, 3 to 1), stomp(5, 5))
        assertEquals(tiles(-1 to 3, 0 to 3, 1 to 3, -1 to 2, -1 to 1), stomp(-2, 4))
        assertEquals(tiles(3 to -1, 2 to -1, 1 to -1, 3 to 0, 3 to 1), stomp(4, -1))
    }

    @Test
    fun `a player level with an edge tile gets the L of the nearest corner`() {
        assertEquals(tiles(-1 to -1, 0 to -1, 1 to -1, -1 to 0, -1 to 1), stomp(0, -1))
        assertEquals(tiles(-1 to -1, 0 to -1, 1 to -1, -1 to 0, -1 to 1), stomp(-1, 0))
        assertEquals(tiles(3 to 3, 2 to 3, 1 to 3, 3 to 2, 3 to 1), stomp(2, 3))
    }

    private fun beneath(cx: Int, cz: Int): Set<CoordGrid> = Brutus.beneathTiles(sw, cx, cz).toSet()

    // live capture, rev 241, 20261012T222638-df49f48: (tick, slam side, player tile, hit?)
    @Test
    fun `a slam hits the tiles under him on its side of the centre`() {
        assertEquals(tiles(0 to 2, 1 to 2, 2 to 2, 0 to 1, 2 to 1), beneath(0, 1))
        assertEquals(tiles(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 2), beneath(-1, 0))
        assertEquals(tiles(2 to 2, 1 to 2, 2 to 1), beneath(1, 1))
        val cases =
            listOf(
                Triple(-1 to 0, 1 to 2, true), // @1384
                Triple(0 to 1, 1 to 1, false), // @1872
                Triple(0 to 1, 1 to 2, true), // @1888
                Triple(0 to 1, 2 to 2, true), // @2316
                Triple(0 to -1, 2 to 1, true), // @2904
                Triple(1 to 1, 2 to 0, false), // @1202
                Triple(1 to 1, 2 to 1, true), // @1955
                Triple(1 to -1, 2 to 2, false), // @2044
                Triple(1 to -1, 1 to 1, false), // @2888
                Triple(1 to -1, 1 to 0, true), // @2896
                Triple(-1 to 1, 0 to 2, true), // @2536
                Triple(-1 to 1, 2 to 2, false), // @2692
                Triple(-1 to -1, 0 to 1, true), // @2764
            )
        for ((side, player, hit) in cases) {
            val (x, z) = player
            assertEquals(hit, sw.translate(x, z) in beneath(side.first, side.second)) { "$side $player" }
        }
    }

    @Test
    fun `every pattern stays outside the boss`() {
        for (x in -4..6) {
            for (z in -4..6) {
                if (x in 0..2 && z in 0..2) continue
                val result = stomp(x, z)
                assert(result.none { it.x - sw.x in 0..2 && it.z - sw.z in 0..2 }) { "($x, $z)" }
                assert(result.size == 3 || result.size == 5) { "($x, $z): $result" }
            }
        }
    }
}
