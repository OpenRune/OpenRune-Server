package org.rsmod.api.testing.factory.map

import org.rsmod.routefinder.collision.CollisionFlagMap

public class TestCollisionFactory {
    /**
     * Creates a new, empty [CollisionFlagMap]. Zones are allocated on demand as flags are written,
     * so this costs only the map's page spine regardless of how much of the world a test touches.
     *
     * @see [borrowSharedMap]
     */
    public fun create(): CollisionFlagMap = CollisionFlagMap()

    /**
     * Returns a shared instance of [CollisionFlagMap] that spans the entire game map.
     *
     * This method provides a pre-allocated [CollisionFlagMap] covering the full extent of the game
     * map, intended for tests needing a complete collision map. The instance is shared across
     * tests, with its zones reset before each use to avoid cross-test contamination.
     *
     * **Note:** Using this shared instance when not necessary may slow down tests due to increased
     * memory usage and potential contention when accessing the shared resource. It is recommended
     * to use [create] instead, unless you specifically require collision data for the entire game
     * map or a large number of zones.
     *
     * @see [create]
     */
    public fun borrowSharedMap(): CollisionFlagMap = borrowFullCollisionMap()

    public companion object {
        private val threadLocalCollision = ThreadLocal.withInitial { CollisionFlagMap() }

        /**
         * Retrieves and resets a shared full collision map from thread-local storage.
         *
         * This method provides a thread-safe way to access a full collision map for testing. The
         * map's flags are reset before returning to ensure no leftover state from previous tests.
         *
         * @return A reset [CollisionFlagMap] spanning the entire game map.
         */
        private fun borrowFullCollisionMap(): CollisionFlagMap =
            threadLocalCollision.get().apply { reset() }
    }
}
