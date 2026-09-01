package org.rsmod.api.mechanics.toxins

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

/**
 * Holds the transient deadline for the next Soulreaper Axe stack decay.
 *
 * The normal axe attack resets this deadline. Behead clears it because it consumes every stack.
 * This uses the game cycle directly instead of an RSCM queue, since rev 240 does not define a
 * Soulreaper-specific queue key.
 */
public object SoulreaperStackDecay {
    // Wiki: "one stack decaying every 50 ticks (30s)" if not actively attacking with the axe.
    public const val INTERVAL_CYCLES: Int = 50

    private val dueAt =
        AttributeKey<Int>(
            resetOnDeath = true,
            temp = true,
        )

    public fun reset(
        player: Player,
        clock: Int = player.currentMapClock,
    ) {
        player.attr[dueAt] = nextDeadline(clock)
    }

    public fun isDue(
        player: Player,
        clock: Int,
    ): Boolean {
        val due = player.attr[dueAt] ?: return false
        return isDue(clock, due)
    }

    public fun clear(player: Player) {
        player.attr.remove(dueAt)
    }

    /** Pure timing math, split out from the [Player]-attached state so it can be unit tested. */
    public fun nextDeadline(clock: Int): Int = clock + INTERVAL_CYCLES

    /** Pure timing math, split out from the [Player]-attached state so it can be unit tested. */
    public fun isDue(
        clock: Int,
        deadline: Int,
    ): Boolean = clock >= deadline
}
