package org.rsmod.content.bosses.leviathan

import kotlin.math.abs
import org.rsmod.api.bosses.runtime.Angles
import org.rsmod.api.bosses.spec.TargetExpr
import org.rsmod.map.CoordGrid

internal object LeviathanArena {
    val BOSS_SPAWN = CoordGrid(2078, 6369, 0)
    const val BOSS_SIZE = 7
    val BOSS_CENTRE: CoordGrid = BOSS_SPAWN.translate(BOSS_SIZE / 2, BOSS_SIZE / 2)

    val HANDHOLDS = CoordGrid(2070, 6368, 0)
    val HANDHOLDS_NOOP = CoordGrid(2091, 6380, 0)
    val HANDHOLDS_INSIDE = CoordGrid(2071, 6368, 0)
    val ISLAND_BOAT = CoordGrid(2065, 6371, 0)

    val SEARCH_SW = CoordGrid(2070, 6361, 0)
    val SEARCH_NE = CoordGrid(2094, 6384, 0)

    val LIGHTNING_HINTS = listOf(CoordGrid(2073, 6372, 0), CoordGrid(2089, 6372, 0))
    val SMOKE_HINTS = listOf(CoordGrid(2081, 6379, 0), CoordGrid(2081, 6365, 0))

    /** Clockwise patrol starting in the north-west corner; coords are the pathfinder's south-west tile. */
    val PATHFINDER_CORNERS =
        listOf(
            CoordGrid(2074, 6377, 0),
            CoordGrid(2086, 6377, 0),
            CoordGrid(2086, 6366, 0),
            CoordGrid(2074, 6366, 0),
        )
    const val PATHFINDER_SIZE = 3

    val LIGHTNING_STRIPS =
        listOf(
            Strip(2073, 6378, 2089, 6381),
            Strip(2072, 6368, 2075, 6381),
            Strip(2073, 6363, 2089, 6366),
            Strip(2087, 6368, 2090, 6381),
        )

    val TAILS =
        listOf(
            Tail("npc.leviathan_tail_ring_1", CoordGrid(2053, 6355, 0), CoordGrid(2067, 6372, 0)),
            Tail("npc.leviathan_tail_ring_2", CoordGrid(2079, 6346, 0), CoordGrid(2081, 6372, 0)),
            Tail("npc.leviathan_tail_ring_1", CoordGrid(2075, 6393, 0), CoordGrid(2073, 6372, 0)),
            Tail("npc.leviathan_tail_ring_1", CoordGrid(2095, 6357, 0), CoordGrid(2081, 6372, 0)),
            Tail("npc.leviathan_tail_ring_3", CoordGrid(2096, 6389, 0), CoordGrid(2081, 6372, 0)),
        )

    data class Strip(val minX: Int, val minZ: Int, val maxX: Int, val maxZ: Int)

    data class Tail(val npc: String, val coords: CoordGrid, val faces: CoordGrid)

    private val EXPLOSION_SPOTANIMS =
        arrayOf(
            "spotanim.vfx_leviathan_explosion_03",
            "spotanim.vfx_leviathan_explosion_04",
            "spotanim.vfx_leviathan_explosion_05",
            "spotanim.vfx_leviathan_explosion_06",
            "spotanim.vfx_leviathan_explosion_07",
            "spotanim.vfx_leviathan_explosion_08",
            "spotanim.vfx_leviathan_explosion_01",
            "spotanim.vfx_leviathan_explosion_02",
        )

    fun relative(static: CoordGrid): TargetExpr.Single = relative(static.x, static.z)

    fun relative(x: Int, z: Int): TargetExpr.Single = TargetExpr.SpawnTile(x - BOSS_SPAWN.x, z - BOSS_SPAWN.z)

    fun smokeDelay(centre: CoordGrid, tile: CoordGrid): Int {
        val manhattan = abs(tile.x - centre.x) + abs(tile.z - centre.z)
        return (manhattan * 3 - 6).coerceAtLeast(0)
    }

    fun smokeSpotanim(centre: CoordGrid, tile: CoordGrid): String {
        val angle = Angles.bearing(centre, tile)
        val octant = ((angle + 128) / 256) % 8
        return EXPLOSION_SPOTANIMS[octant]
    }
}

internal class Arena(private val dx: Int, private val dz: Int, private val level: Int) {
    fun at(static: CoordGrid): CoordGrid = CoordGrid(static.x + dx, static.z + dz, level)

    fun toStatic(coords: CoordGrid): CoordGrid = CoordGrid(coords.x - dx, coords.z - dz, 0)

    val centre: CoordGrid get() = at(LeviathanArena.BOSS_CENTRE)

    fun inStrip(coords: CoordGrid, strip: LeviathanArena.Strip): Boolean {
        val s = toStatic(coords)
        return s.x in strip.minX..strip.maxX && s.z in strip.minZ..strip.maxZ
    }

    fun inSearchBox(coords: CoordGrid): Boolean {
        val s = toStatic(coords)
        return s.x in LeviathanArena.SEARCH_SW.x..LeviathanArena.SEARCH_NE.x &&
            s.z in LeviathanArena.SEARCH_SW.z..LeviathanArena.SEARCH_NE.z
    }

    companion object {
        fun forBoss(spawn: CoordGrid): Arena =
            Arena(
                spawn.x - LeviathanArena.BOSS_SPAWN.x,
                spawn.z - LeviathanArena.BOSS_SPAWN.z,
                spawn.level,
            )
    }
}
