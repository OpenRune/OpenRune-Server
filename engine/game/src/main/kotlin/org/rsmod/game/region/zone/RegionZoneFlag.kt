@file:Suppress("UnusedImport")

package org.rsmod.game.region.zone

import org.rsmod.game.region.util.NormalRegionZoneMap.NormalZone

/**
 * Represents a unique flag that can be assigned to a [RegionZoneCopy].
 *
 * Each `RegionZoneCopy` can store a **single** unique flag, allowing differentiation between
 * identical copied zones that serve different purposes.
 *
 * These flags are particularly useful in scenarios where multiple instances of the same copied zone
 * exist but need to be treated differently by content.
 *
 * For example, a minigame home base may copy the same zone for both a red team and a blue team, but
 * in separate map sections. Flags allow distinguishing between them.
 *
 * The value `0` is reserved for "no flag", so the three-bit unique-flag field supports seven
 * distinct flags.
 */
public enum class RegionZoneFlag(private val value: Int) {
    Flag1(1),
    Flag2(2),
    Flag3(3),
    Flag4(4),
    Flag5(5),
    Flag6(6),
    Flag7(7);

    public val bitmask: Int
        get() = value
}
