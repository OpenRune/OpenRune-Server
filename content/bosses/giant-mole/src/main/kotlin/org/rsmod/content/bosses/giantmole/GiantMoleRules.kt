package org.rsmod.content.bosses.giantmole

import org.rsmod.game.hit.HitType
import org.rsmod.game.map.collision.get
import org.rsmod.map.CoordGrid
import org.rsmod.routefinder.collision.CollisionFlagMap
import org.rsmod.routefinder.flag.CollisionFlag

internal object GiantMoleRules {
    const val NPC_SIZE = 3
    const val BURROW_CHANCE = 4
    const val BURROW_DELAY_TICKS = 2

    val SPAWN = CoordGrid(1759, 5189)

    val MOLE_HILLS =
        listOf(
            CoordGrid(2984, 3387),
            CoordGrid(2987, 3387),
            CoordGrid(2999, 3375),
            CoordGrid(2996, 3377),
            CoordGrid(3005, 3376),
            CoordGrid(2989, 3378),
        )

    // The wiki strategy map lists the lair points; a size-3 footprint is measured from its
    // south-west corner.
    val BURROW_POINTS =
        listOf(
            CoordGrid(1739, 5220),
            CoordGrid(1775, 5232),
            CoordGrid(1737, 5208),
            CoordGrid(1778, 5207),
            CoordGrid(1753, 5197),
            CoordGrid(1778, 5187),
            CoordGrid(1741, 5177),
            CoordGrid(1758, 5179),
            CoordGrid(1746, 5170),
            CoordGrid(1775, 5173),
            CoordGrid(1778, 5164),
            CoordGrid(1760, 5163),
            CoordGrid(1753, 5150),
        )

    val OPEN_LIGHT_SOURCES =
        mapOf(
            "obj.lit_candle" to "obj.unlit_candle",
            "obj.lit_black_candle" to "obj.unlit_black_candle",
            "obj.torch_lit" to "obj.torch_unlit",
            "obj.oil_lamp_lit" to "obj.oil_lamp_unlit",
        )

    private const val LAIR_MIN_X = 1728
    private const val LAIR_MAX_X = 1791
    private const val LAIR_MIN_Z = 5120
    private const val LAIR_MAX_Z = 5247

    fun inMoleLair(coords: CoordGrid): Boolean =
        coords.level == 0 &&
            coords.x in LAIR_MIN_X..LAIR_MAX_X &&
            coords.z in LAIR_MIN_Z..LAIR_MAX_Z

    fun onMoleHill(coords: CoordGrid): Boolean =
        coords.level == 0 && MOLE_HILLS.any { it.chebyshevDistance(coords) <= 1 }

    fun canBurrow(hitpoints: Int, maxHitpoints: Int): Boolean =
        hitpoints > maxHitpoints / 20 && hitpoints <= maxHitpoints / 2

    fun isMagicSplash(type: HitType, damage: Int): Boolean =
        type == HitType.Magic && damage == 0

    fun distance(origin: CoordGrid, size: Int, target: CoordGrid): Int {
        if (origin.level != target.level) {
            return Int.MAX_VALUE
        }
        return maxOf(
            (origin.x - target.x).coerceAtLeast(0),
            (target.x - origin.x - size + 1).coerceAtLeast(0),
            (origin.z - target.z).coerceAtLeast(0),
            (target.z - origin.z - size + 1).coerceAtLeast(0),
        )
    }

    fun withinMeleeRange(origin: CoordGrid, target: CoordGrid): Boolean =
        distance(origin, NPC_SIZE, target) <= 1

    fun isWalkable(collision: CollisionFlagMap, origin: CoordGrid, size: Int): Boolean {
        for (dx in 0 until size) {
            for (dz in 0 until size) {
                val tile = origin.translate(dx, dz)
                val flags = collision[tile]
                if (flags and (CollisionFlag.BLOCK_WALK or CollisionFlag.LOC) != 0) {
                    return false
                }
            }
        }
        return true
    }

    fun burrowCandidates(
        collision: CollisionFlagMap,
        current: CoordGrid,
        blocked: Collection<CoordGrid> = emptyList(),
    ): List<CoordGrid> =
        BURROW_POINTS.filter { candidate ->
            candidate != current &&
                isWalkable(collision, candidate, NPC_SIZE) &&
                blocked.none { it.chebyshevDistance(candidate) <= 2 }
        }

    fun openLightTarget(lit: String): String? = OPEN_LIGHT_SOURCES[lit]
}
