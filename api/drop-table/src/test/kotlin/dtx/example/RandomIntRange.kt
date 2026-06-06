package dtx.example

import kotlin.random.Random

@JvmInline
value class RandomIntRange(val wrappedRange: IntRange): ClosedRange<Int> by wrappedRange, OpenEndRange<Int> by wrappedRange, Iterable<Int> by wrappedRange {

    override val start: Int get() = wrappedRange.first

    override val endInclusive: Int get() = wrappedRange.endInclusive

    override fun contains(value: Int): Boolean {
        return wrappedRange.contains(value)
    }

    override fun isEmpty(): Boolean {
        return wrappedRange.isEmpty()
    }

    fun random(withRandom: Random = Random): Int {
        return wrappedRange.random(withRandom)
    }
}

inline fun IntRange.toRandomIntRange(): RandomIntRange {
    return RandomIntRange(this)
}

infix fun Int.randTo(endInclusive: Int): RandomIntRange {
    return RandomIntRange(this .. endInclusive)
}
