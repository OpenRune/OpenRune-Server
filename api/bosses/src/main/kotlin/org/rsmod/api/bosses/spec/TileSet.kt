package org.rsmod.api.bosses.spec

/** An inclusive box between two corner tiles; spawn-relative corners keep it instance-safe. */
data class Area(val sw: TargetExpr.Single, val ne: TargetExpr.Single)

/**
 * A set of tiles for [Effect.OnTiles], resolved when it runs. Only free tiles are kept: inside
 * [area], walkable, and not already holding a loc the encounter spawned.
 */
sealed interface TileSet {
    val area: Area

    data class RandomFree(override val area: Area, val count: IntRange) : TileSet

    /** The tile under each player inside [area]. */
    data class UnderPlayers(override val area: Area) : TileSet

    /** Each of [tiles], or the nearest free tile within [searchRadius] of it. */
    data class Nearest(
        val tiles: List<TargetExpr.Single>,
        override val area: Area,
        val searchRadius: Int,
    ) : TileSet
}
