package org.rsmod.routefinder.flag

/** @author Kris | 15/01/2022 */
public object BlockAccessFlag {
    public const val NORTH: Int = 0x1
    public const val EAST: Int = 0x2
    public const val SOUTH: Int = 0x4
    public const val WEST: Int = 0x8

    /**
     * The compass approaches, and the only bits [org.rsmod.routefinder.util.Rotations.rotate] acts
     * on.
     */
    public const val DIRECTIONS: Int = NORTH or EAST or SOUTH or WEST

    /**
     * Every approach position a loc can block. A restricted loc clears exactly one bit - the side
     * it may be approached from. The fifth position is not a compass direction and is not modelled,
     * so locs that clear it cannot be reached.
     */
    public const val ALL_APPROACHES: Int = 0x1F
}
