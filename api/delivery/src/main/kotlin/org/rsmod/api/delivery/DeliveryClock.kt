package org.rsmod.api.delivery

import jakarta.inject.Singleton

/**
 * Use a separate monotonic cycle counter in memory and wall-clock deadlines in persistence so
 * delays survive restarts.
 */
@Singleton
public class DeliveryClock {
    public var cycle: Int = 0
        private set

    internal fun tick() {
        cycle++
    }
}
