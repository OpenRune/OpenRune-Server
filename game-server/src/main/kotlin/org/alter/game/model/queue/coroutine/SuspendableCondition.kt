package org.alter.game.model.queue.coroutine

import gg.rsmod.util.toStringHelper
import java.util.concurrent.atomic.AtomicInteger

/**
 * A condition that must be met for a suspended coroutine to continue.
 *
 * @author Tom <rspsmods@gmail.com>
 */
abstract class SuspendableCondition {
    /**
     * Whether or not the coroutine can continue its logic.
     */
    abstract fun resume(): Boolean
}

/**
 * A [SuspendableCondition] that waits for the given amount of cycles before
 * permitting the coroutine to continue its logic.
 *
 * @param cycles
 * The amount of game cycles that must pass before the coroutine can continue.
 */
class WaitCondition(cycles: Int) : SuspendableCondition() {
    private val cyclesLeft = AtomicInteger(cycles)
    private var hasBeenChecked = AtomicInteger(0) // Track if resume() has been called at least once

    override fun resume(): Boolean {
        val checkCount = hasBeenChecked.getAndIncrement()

        // On the first call (same tick as wait), return false to wait for next tick
        // On subsequent calls, decrement and check if ready
        return if (checkCount == 0) {
            // First call in the same tick - don't resume yet, but decrement for next tick
            cyclesLeft.decrementAndGet()
            false
        } else {
            // Subsequent calls - decrement and check if cyclesLeft < 0
            cyclesLeft.decrementAndGet() < 0
        }
    }

    override fun toString(): String = toStringHelper().add("cycles", cyclesLeft).toString()
}

/**
 * A [SuspendableCondition] that waits for [predicate] to return true before
 * permitting the coroutine to continue its logic.
 */
class PredicateCondition(private val predicate: () -> Boolean) : SuspendableCondition() {
    override fun resume(): Boolean = predicate.invoke()
}
