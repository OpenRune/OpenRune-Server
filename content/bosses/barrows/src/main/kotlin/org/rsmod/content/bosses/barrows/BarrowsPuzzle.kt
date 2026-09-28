package org.rsmod.content.bosses.barrows

internal enum class BarrowsPuzzleType(
    val answer: Int,
    val sequence: List<Int>,
    val options: List<Int>,
) {
    ARROWS(6713, listOf(6716, 6717, 6718), listOf(6713, 6714, 6715)),
    SQUARES(6719, listOf(6722, 6723, 6724), listOf(6719, 6720, 6721)),
    SQUARES_OFFSET(6725, listOf(6728, 6729, 6730), listOf(6725, 6726, 6727)),
    SHAPES(6731, listOf(6734, 6735, 6736), listOf(6731, 6732, 6733)),
}

internal class BarrowsPuzzle(val type: BarrowsPuzzleType, val options: List<Int>) {
    val correctSlot: Int = options.indexOf(type.answer)

    companion object {
        fun random(): BarrowsPuzzle {
            val type = BarrowsPuzzleType.entries.random()
            return BarrowsPuzzle(type, type.options.shuffled())
        }
    }
}
