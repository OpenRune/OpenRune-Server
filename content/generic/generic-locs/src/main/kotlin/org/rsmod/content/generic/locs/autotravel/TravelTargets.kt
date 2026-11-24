package org.rsmod.content.generic.locs.autotravel

import org.rsmod.map.CoordGrid

data class TravelLoc(val id: Int, val coords: CoordGrid, val angle: Int)

data class TravelType(val name: String, val ops: List<String>, val width: Int, val length: Int) {
    fun op(index: Int): String = ops.getOrNull(index)?.trim()?.lowercase().orEmpty()

    fun hasOp(text: String): Boolean = (0 until 5).any { op(it) == text }
}

enum class TravelKind(val content: String) {
    Up("content.auto_travel_up"),
    Down("content.auto_travel_down"),
    Option("content.auto_travel_option"),
    Cross("content.auto_travel_cross"),
}

/**
 * Staircases, gangplanks and open trapdoors come in more shapes than the hand-measured staircase
 * scripts cover, so these are served by finding the other end at click time: a matching loc one
 * level up or down nearby - or 6400 tiles away, where a dungeon sits under the surface - and
 * landing on the standable tile beside it nearest to where the player stood.
 */
object TravelTargets {
    const val DUNGEON_OFFSET = 6400
    private const val SEARCH_RADIUS = 4
    private const val CROSS_RADIUS = 6
    private const val MAX_LEVEL = 3
    private const val MAX_COORD = 16383

    private val STAIR_WORDS = listOf("stair", "steps", "trapdoor")

    fun kindOf(type: TravelType): TravelKind? {
        val name = type.name.lowercase()
        if ("gangplank" in name && type.op(0) == "cross") return TravelKind.Cross
        if (STAIR_WORDS.none { it in name }) return null
        return when {
            type.op(0) == "climb-up" && !type.hasOp("climb-down") -> TravelKind.Up
            type.op(0) == "climb-down" && !type.hasOp("climb-up") -> TravelKind.Down
            type.op(0) == "climb" && type.op(1) == "climb-up" && type.op(2) == "climb-down" ->
                TravelKind.Option
            else -> null
        }
    }

    /**
     * The loc at the other end of a climb ([up] true or false) or a gangplank crossing, searched
     * around [from] on the levels that kind of travel reaches.
     */
    fun counterpart(
        from: TravelLoc,
        kind: TravelKind,
        up: Boolean,
        locsAt: (CoordGrid) -> List<TravelLoc>,
        type: (Int) -> TravelType?,
    ): TravelLoc? {
        val origin = from.coords
        if (kind == TravelKind.Cross) {
            val gangplank = { t: TravelType -> "gangplank" in t.name.lowercase() }
            return listOf(origin.level + 1, origin.level - 1)
                .filter { it in 0..MAX_LEVEL }
                .firstNotNullOfOrNull { level ->
                    nearest(CoordGrid(origin.x, origin.z, level), CROSS_RADIUS, locsAt, type, gangplank)
                }
        }
        val wanted: (TravelType) -> Boolean =
            if (up) {
                { t -> t.hasOp("climb-down") || t.hasOp("climb") || "trapdoor" in t.name.lowercase() }
            } else {
                { t -> t.hasOp("climb-up") || t.hasOp("climb") }
            }
        val level = origin.level + if (up) 1 else -1
        if (level in 0..MAX_LEVEL) {
            nearest(CoordGrid(origin.x, origin.z, level), SEARCH_RADIUS, locsAt, type, wanted)
                ?.let { return it }
        }
        val dungeonZ = origin.z + if (up) -DUNGEON_OFFSET else DUNGEON_OFFSET
        if (dungeonZ !in 0..MAX_COORD) return null
        return nearest(CoordGrid(origin.x, dungeonZ, origin.level), SEARCH_RADIUS, locsAt, type, wanted)
    }

    /**
     * The standable tile touching [target]'s footprint nearest to where [standing] sits relative to
     * [from], carried over to [target] - so a player who climbs from the south of a staircase
     * steps off on the south side of the other end, even when that end is in a dungeon.
     */
    fun landing(
        from: TravelLoc,
        target: TravelLoc,
        targetType: TravelType,
        standing: CoordGrid,
        standable: (CoordGrid) -> Boolean,
    ): CoordGrid? {
        val turned = target.angle % 2 == 1
        val sizeX = if (turned) targetType.length else targetType.width
        val sizeZ = if (turned) targetType.width else targetType.length
        val origin = target.coords
        val ring = mutableListOf<CoordGrid>()
        for (x in origin.x - 1..origin.x + sizeX) {
            for (z in origin.z - 1..origin.z + sizeZ) {
                val inside = x in origin.x until origin.x + sizeX && z in origin.z until origin.z + sizeZ
                if (!inside) ring += CoordGrid(x, z, origin.level)
            }
        }
        val projectedX = origin.x + (standing.x - from.coords.x)
        val projectedZ = origin.z + (standing.z - from.coords.z)
        return ring
            .filter(standable)
            .minWithOrNull(
                compareBy<CoordGrid> { squaredDistance(it.x, it.z, projectedX, projectedZ) }
                    .thenBy { it.x }
                    .thenBy { it.z }
            )
    }

    private fun nearest(
        centre: CoordGrid,
        radius: Int,
        locsAt: (CoordGrid) -> List<TravelLoc>,
        type: (Int) -> TravelType?,
        matches: (TravelType) -> Boolean,
    ): TravelLoc? {
        var best: TravelLoc? = null
        var bestDistance = Int.MAX_VALUE
        for (dx in -radius..radius) {
            for (dz in -radius..radius) {
                val x = centre.x + dx
                val z = centre.z + dz
                if (x !in 0..MAX_COORD || z !in 0..MAX_COORD) continue
                for (loc in locsAt(CoordGrid(x, z, centre.level))) {
                    val t = type(loc.id) ?: continue
                    if (!matches(t)) continue
                    val distance = dx * dx + dz * dz
                    if (distance < bestDistance) {
                        best = loc
                        bestDistance = distance
                    }
                }
            }
        }
        return best
    }

    private fun squaredDistance(ax: Int, az: Int, bx: Int, bz: Int): Int =
        (ax - bx) * (ax - bx) + (az - bz) * (az - bz)
}
