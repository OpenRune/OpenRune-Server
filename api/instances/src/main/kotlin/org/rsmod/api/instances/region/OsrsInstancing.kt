package org.rsmod.api.instances.region

import org.rsmod.api.registry.region.RegionRegistry

/**
 * Jagex instanced map layout, as observed post-Sailing.
 *
 * The map spans `0,0` to `16383,16383` inclusive. Static overworld occupies `x < 6400`; everything
 * from `x >= 6400` is copied space, divided into vertical bands that each span the full `z` axis:
 *
 * | Band         | X range       | Square | Grid    |
 * |--------------|---------------|--------|---------|
 * | Large copies | 6400 - 10239  | 320    | 10 x 42 |
 * | Small copies | 10240 - 14079 | 128    | 20 x 85 |
 * | World entity | 14080 - 16383 | 64     | 36 x 256|
 *
 * Large copies back Soul Wars, Last Man Standing and the Whisperer area; small copies back
 * virtually everything else. Regions are separated by 64 tiles so neighbouring copies are not
 * visible from one another, except world entity copies, which are packed with no gap.
 *
 * World entity copies are reserved for player-owned boats. They also differ in lifecycle: unlike
 * the other two bands they are not destroyed when they hold no players, so the caller that
 * allocates one is responsible for releasing it.
 *
 * The client applies zones via **Rebuild Region** in 8x8 chunks, rotated 0, 90, 180 or 270 degrees.
 * Engine slot allocation lives in [RegionRegistry]; the values here mirror it.
 */
public object OsrsInstancing {
    public const val STATIC_MAP_MAX_X: Int = 6399

    public const val INSTANCE_MIN_X: Int = RegionRegistry.INSTANCE_MIN_X

    public const val LARGE_MIN_X: Int = RegionRegistry.LARGE_MIN_X

    public const val SMALL_MIN_X: Int = RegionRegistry.SMALL_MIN_X

    public const val WORLDENTITY_MIN_X: Int = RegionRegistry.WORLDENTITY_MIN_X

    public const val MAX_CONCURRENT_SMALL: Int = RegionRegistry.MAX_CONCURRENT_SMALL_REGIONS

    public const val MAX_CONCURRENT_LARGE: Int = RegionRegistry.MAX_CONCURRENT_LARGE_REGIONS

    public const val MAX_CONCURRENT_WORLDENTITY: Int =
        RegionRegistry.MAX_CONCURRENT_WORLDENTITY_REGIONS

    public const val SMALL_BUILD_SQUARES: Int = RegionRegistry.SMALL_REGION_SQUARE_LENGTH

    public const val LARGE_BUILD_SQUARES: Int = RegionRegistry.LARGE_REGION_SQUARE_LENGTH

    public const val WORLDENTITY_BUILD_SQUARES: Int =
        RegionRegistry.WORLDENTITY_REGION_SQUARE_LENGTH

    public const val PADDING_BETWEEN_INSTANCES: Int = RegionRegistry.PADDING_SQUARES * 2

    public const val ZONE_SIZE: Int = MAP_SQUARE_ZONE_LENGTH

    public fun isInstancedWorldX(x: Int): Boolean = x >= INSTANCE_MIN_X

    public fun isLargeInstanceWorldX(x: Int): Boolean = x in LARGE_MIN_X until SMALL_MIN_X

    public fun isSmallInstanceWorldX(x: Int): Boolean = x in SMALL_MIN_X until WORLDENTITY_MIN_X

    public fun isWorldEntityInstanceWorldX(x: Int): Boolean = x >= WORLDENTITY_MIN_X
}
