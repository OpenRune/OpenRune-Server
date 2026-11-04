package org.rsmod.api.delivery

import org.rsmod.game.entity.Player

/**
 * The line id is persistent: renaming strands saved parcels. Commit arrivals before callbacks.
 * Frozen lines retain due times unless pauseWhileFrozen is enabled for a physical conveyor.
 */
public class DeliveryLine(
    public val id: String,
    public val delayTicks: Int,
    public val capacity: (Player) -> Int,
    public val onArrival: (Player, DeliveryArrival) -> Unit,
    public val maxPending: Int = DEFAULT_MAX_PENDING,
    public val frozen: () -> Boolean = { false },
    public val pauseWhileFrozen: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "`id` must not be blank." }
        require(delayTicks >= 0) { "`delayTicks` must not be negative: $delayTicks (id=$id)" }
        require(maxPending >= 1) { "`maxPending` must be positive: $maxPending (id=$id)" }
    }

    override fun toString(): String = "DeliveryLine(id=$id, delayTicks=$delayTicks)"

    public companion object {
        public const val DEFAULT_MAX_PENDING: Int = 512
    }
}
