package org.rsmod.content.bosses.archaeologists

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.random.DefaultGameRandom
import org.rsmod.map.CoordGrid

class BookRainTest {
    private val centre = CoordGrid(3200, 3200)
    private val random = DefaultGameRandom(Random(0))

    @Test
    fun `every book gets a different tile`() {
        val first =
            chooseBookTiles(
                centre = centre,
                count = 3,
                radius = 2,
                exclude = emptySet(),
                random = random,
            )
        val second =
            chooseBookTiles(
                centre = centre,
                count = 2,
                radius = 2,
                exclude = first.toSet(),
                random = random,
            )

        assertEquals(3, first.size)
        assertEquals(2, second.size)
        assertTrue(first.intersect(second.toSet()).isEmpty())
        assertTrue((first + second).none { it == centre })
        assertTrue((first + second).all { it.chebyshevDistance(centre) <= 2 })
    }

    @Test
    fun `explosions only affect the impact tile and its neighbours`() {
        val config = BookRainConfig(directMaxHit = 56, splashMaxHit = 18)

        assertEquals(56, explosionMaxHit(0, config) ?: -1)
        assertEquals(18, explosionMaxHit(1, config) ?: -1)
        assertNull(explosionMaxHit(2, config))
    }
}
