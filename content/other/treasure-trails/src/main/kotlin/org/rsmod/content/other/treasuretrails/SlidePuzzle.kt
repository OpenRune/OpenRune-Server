package org.rsmod.content.other.treasuretrails

import kotlin.random.Random

object SlidePuzzle {
    const val WIDTH = 5
    const val CELLS = WIDTH * WIDTH
    const val TILES = CELLS - 1
    const val EMPTY = -1

    fun solved(): IntArray = IntArray(CELLS) { if (it == CELLS - 1) EMPTY else it }

    fun isSolved(board: IntArray): Boolean = board.contentEquals(solved())

    fun scrambled(random: Random, moves: Int = SCRAMBLE_MOVES): IntArray {
        val board = solved()
        var gap = board.indexOf(EMPTY)
        var previous = -1
        repeat(moves) {
            val options = neighbours(gap).filter { it != previous }
            val cell = options[random.nextInt(options.size)]
            board[gap] = board[cell]
            board[cell] = EMPTY
            previous = gap
            gap = cell
        }
        if (isSolved(board)) return scrambled(random, moves)
        return board
    }

    fun slide(board: IntArray, cell: Int): Boolean {
        if (cell !in board.indices || board[cell] == EMPTY) return false
        val gap = neighbours(cell).firstOrNull { board[it] == EMPTY } ?: return false
        board[gap] = board[cell]
        board[cell] = EMPTY
        return true
    }

    private fun neighbours(cell: Int): List<Int> {
        val x = cell % WIDTH
        val z = cell / WIDTH
        return buildList {
            if (x > 0) add(cell - 1)
            if (x < WIDTH - 1) add(cell + 1)
            if (z > 0) add(cell - WIDTH)
            if (z < WIDTH - 1) add(cell + WIDTH)
        }
    }

    private const val SCRAMBLE_MOVES = 200
}
