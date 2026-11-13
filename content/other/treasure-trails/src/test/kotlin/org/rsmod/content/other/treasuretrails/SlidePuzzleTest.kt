package org.rsmod.content.other.treasuretrails

import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SlidePuzzleTest {
    @Test
    fun `scrambled boards keep every tile once, stay solvable and are never already solved`() {
        repeat(200) { seed ->
            val board = SlidePuzzle.scrambled(Random(seed))
            assertEquals((0 until SlidePuzzle.TILES).toList(), board.filter { it >= 0 }.sorted())
            assertEquals(1, board.count { it == SlidePuzzle.EMPTY })
            assertFalse(SlidePuzzle.isSolved(board))
            assertEquals(0, inversions(board) % 2, "seed=$seed")
        }
    }

    @Test
    fun `only a tile next to the gap slides`() {
        val board = SlidePuzzle.solved()
        assertFalse(SlidePuzzle.slide(board, 0))
        assertFalse(SlidePuzzle.slide(board, SlidePuzzle.CELLS - 1))
        assertTrue(SlidePuzzle.slide(board, SlidePuzzle.CELLS - 2))
        assertEquals(SlidePuzzle.EMPTY, board[SlidePuzzle.CELLS - 2])
        assertEquals(SlidePuzzle.CELLS - 2, board[SlidePuzzle.CELLS - 1])
        assertTrue(SlidePuzzle.slide(board, SlidePuzzle.CELLS - 1))
        assertTrue(SlidePuzzle.isSolved(board))
    }

    @Test
    fun `sliding never creates or loses a tile`() {
        val random = Random(11)
        val board = SlidePuzzle.scrambled(random)
        repeat(500) { SlidePuzzle.slide(board, random.nextInt(SlidePuzzle.CELLS)) }
        assertEquals((0 until SlidePuzzle.TILES).toList(), board.filter { it >= 0 }.sorted())
        assertEquals(1, board.count { it == SlidePuzzle.EMPTY })
    }

    private fun inversions(board: IntArray): Int {
        val tiles = board.filter { it != SlidePuzzle.EMPTY }
        var count = 0
        for (i in tiles.indices) for (j in i + 1 until tiles.size) if (tiles[i] > tiles[j]) count++
        return count
    }
}
