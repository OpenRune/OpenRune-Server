package org.rsmod.api.instances.region

import org.rsmod.api.registry.region.RegionRegistry

/**
 * Jagex instanced map layout as documented in [OSRS instancing mechanics](https://osrs-docs.com/docs/mechanics/instancing/).
 *
 * Static overworld occupies `x < 6400`. Copied regions are allocated from `x >= 6400` with padding
 * so adjacent instances are not visible. Small copies use a fixed 128×128 build square; large copies
 * use 320×320 (Soul Wars, Last Man Standing). The client applies zones via **Rebuild Region** in 8×8
 * chunks with rotation at 0°, 90°, 180°, or 270°.
 *
 * Engine slot allocation is implemented in [RegionRegistry]; these values mirror the live game caps
 * and coordinate rules.
 */
public object OsrsInstancing {
    public const val STATIC_MAP_MAX_X: Int = 6399

    public const val INSTANCE_MIN_X: Int = 6400

    public const val SMALL_LARGE_Z_SPLIT: Int = 5248

    public const val MAX_CONCURRENT_SMALL: Int = RegionRegistry.MAX_CONCURRENT_SMALL_REGIONS

    public const val MAX_CONCURRENT_LARGE: Int = RegionRegistry.MAX_CONCURRENT_LARGE_REGIONS

    public const val SMALL_BUILD_SQUARES: Int = RegionRegistry.SMALL_REGION_SQUARE_LENGTH

    public const val LARGE_BUILD_SQUARES: Int = RegionRegistry.LARGE_REGION_SQUARE_LENGTH

    public const val PADDING_BETWEEN_INSTANCES: Int = RegionRegistry.PADDING_SQUARES * 2

    public const val ZONE_SIZE: Int = MAP_SQUARE_ZONE_LENGTH

    public fun isInstancedWorldX(x: Int): Boolean = x >= INSTANCE_MIN_X

    public fun isSmallInstanceWorldZ(z: Int): Boolean = z < SMALL_LARGE_Z_SPLIT

    public fun isLargeInstanceWorldZ(z: Int): Boolean = z >= SMALL_LARGE_Z_SPLIT
}
