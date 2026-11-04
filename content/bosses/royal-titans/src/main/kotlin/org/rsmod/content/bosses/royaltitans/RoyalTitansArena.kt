package org.rsmod.content.bosses.royaltitans

import org.rsmod.game.entity.Npc
import org.rsmod.map.CoordGrid

/**
 * Geometry of the Royal Titans arena, expressed as offsets from a titan's spawn tile so the same
 * numbers work in the relocated instance copy.
 *
 * The walkable corridor is five wide. In phase one the titans stand on the side ledges and project a
 * three-row strip in front of themselves, five columns wide. In phase two both titans stand on the
 * three rows at either end of the corridor, which blocks those rows and leaves a seven-row middle
 * band where the fight happens.
 */
internal object RoyalTitansArena {
    const val STOMP_PHASE_ONE_DEPTH = 3
    const val STOMP_PHASE_TWO_DEPTH = 7
    const val PHASE_TWO_END_DEPTH = 3
    const val BAND_SOUTH = -2
    const val BAND_NORTH = 4
    const val CENTRE_SOUTH = 0
    const val CENTRE_NORTH = 2
    const val MINION_ROW_MIN = -5
    const val MINION_ROW_MAX = 5
    const val WALL_ROW_MIN = -5
    const val WALL_ROW_MAX = 7

    fun columns(spawn: CoordGrid, element: TitanElement): IntRange =
        if (element == TitanElement.Fire) {
            spawn.x + 3..spawn.x + 7
        } else {
            spawn.x - 5..spawn.x - 1
        }

    fun phaseTwoPosition(npc: Npc, element: TitanElement): CoordGrid =
        phaseTwoPosition(npc.spawnCoords, element)

    fun phaseTwoPosition(spawn: CoordGrid, element: TitanElement): CoordGrid =
        if (element == TitanElement.Fire) {
            CoordGrid(spawn.x + 4, spawn.z + 5, spawn.level)
        } else {
            CoordGrid(spawn.x - 4, spawn.z - 5, spawn.level)
        }

    fun endTiles(spawn: CoordGrid, element: TitanElement): List<CoordGrid> {
        val level = spawn.level
        val xs = columns(spawn, element)
        val zs =
            if (element == TitanElement.Fire) {
                spawn.z + 5..spawn.z + 7
            } else {
                spawn.z - 5..spawn.z - 3
            }
        return buildList(xs.count() * zs.count()) {
            for (x in xs) {
                for (z in zs) {
                    add(CoordGrid(x, z, level))
                }
            }
        }
    }

    /** The seven-row middle band that the elemental blast and the phase-two stomp cover. */
    fun bandTiles(npc: Npc, element: TitanElement): List<CoordGrid> = bandTiles(npc.spawnCoords, element)

    fun bandTiles(spawn: CoordGrid, element: TitanElement): List<CoordGrid> =
        tiles(spawn, element, spawn.z + BAND_SOUTH, spawn.z + BAND_NORTH)

    /** The central rows a phase-one safe tile is drawn from. */
    fun centreTiles(spawn: CoordGrid, element: TitanElement): List<CoordGrid> =
        tiles(spawn, element, spawn.z + CENTRE_SOUTH, spawn.z + CENTRE_NORTH)

    /** The five-by-three strip a phase-one stomp projects into the corridor. */
    fun stompTiles(npc: Npc): Collection<CoordGrid> {
        val element = TitanElement.of(npc) ?: return emptyList()
        val coords = npc.coords
        val level = coords.level
        val size = npc.size
        val eastward = element == TitanElement.Fire
        val near = if (eastward) coords.x + size else coords.x - 1
        val step = if (eastward) 1 else -1
        val startZ = coords.z + (size - STOMP_PHASE_ONE_DEPTH) / 2
        return buildList(STOMP_PHASE_ONE_DEPTH * STOMP_PHASE_ONE_DEPTH) {
            for (depth in 0 until STOMP_PHASE_ONE_DEPTH) {
                val x = near + depth * step
                for (dz in 0 until STOMP_PHASE_ONE_DEPTH) {
                    add(CoordGrid(x, startZ + dz, level))
                }
            }
        }
    }

    /** The band grouped by distance from the titan, nearest row first, for the stomp ground effect. */
    fun phaseTwoStompRows(npc: Npc, element: TitanElement): List<List<CoordGrid>> =
        phaseTwoStompRows(npc.spawnCoords, element)

    fun phaseTwoStompRows(spawn: CoordGrid, element: TitanElement): List<List<CoordGrid>> {
        val rows = LinkedHashMap<Int, MutableList<CoordGrid>>()
        for (tile in bandTiles(spawn, element)) {
            val depth =
                if (element == TitanElement.Fire) {
                    spawn.z + BAND_NORTH - tile.z
                } else {
                    tile.z - (spawn.z + BAND_SOUTH)
                }
            rows.getOrPut(depth) { mutableListOf() }.add(tile)
        }
        return rows.toSortedMap().values.toList()
    }

    fun safeTile(
        spawn: CoordGrid,
        element: TitanElement,
        survivor: TitanElement?,
        roll: (IntRange) -> Int,
    ): CoordGrid {
        val x = columns(spawn, element).first + roll(0..4)
        val z =
            when (survivor) {
                // The surviving titan places it on the row furthest from itself.
                TitanElement.Fire -> spawn.z + BAND_SOUTH
                TitanElement.Ice -> spawn.z + BAND_NORTH
                else -> spawn.z + CENTRE_SOUTH + roll(0..2)
            }
        return CoordGrid(x, z, spawn.level)
    }

    /**
     * The tiles of one elemental blast. With both titans alive the fire blast owns everything north
     * of the safe tile (and the tiles east of it on its row), the ice blast the rest. A lone titan
     * blasts the whole band except the safe tile.
     */
    fun waveTiles(
        npc: Npc,
        element: TitanElement,
        safe: CoordGrid,
        survivor: TitanElement?,
    ): List<CoordGrid> = waveTiles(npc.spawnCoords, element, safe, survivor)

    fun waveTiles(
        spawn: CoordGrid,
        element: TitanElement,
        safe: CoordGrid,
        survivor: TitanElement?,
    ): List<CoordGrid> {
        val band = bandTiles(spawn, element)
        if (survivor != null) {
            return band.filter { it != safe }
        }
        return band.filter { tile ->
            if (tile == safe) {
                false
            } else if (element == TitanElement.Fire) {
                tile.z > safe.z || (tile.z == safe.z && tile.x > safe.x)
            } else {
                tile.z < safe.z || (tile.z == safe.z && tile.x < safe.x)
            }
        }
    }

    fun elementalSummonTiles(
        npc: Npc,
        element: TitanElement,
        phaseTwo: Boolean = false,
        roll: (IntRange) -> Int,
    ): List<CoordGrid> = elementalSummonTiles(npc.spawnCoords, element, phaseTwo, roll)

    fun elementalSummonTiles(
        spawn: CoordGrid,
        element: TitanElement,
        phaseTwo: Boolean = false,
        roll: (IntRange) -> Int,
    ): List<CoordGrid> {
        // The capture only ever spawns elementals in the five-column corridor, never on the
        // side ledges. Roll within the titan's own column strip instead of a raw offset range.
        val column = columns(spawn, element).first + roll(0..4)
        val startRow =
            if (phaseTwo) {
                // Phase two elementals stay inside the seven-row middle band and must never
                // spawn on the blocked end-zone tiles.
                val south = spawn.z + BAND_SOUTH
                south + roll(0..(BAND_NORTH - BAND_SOUTH - 2))
            } else {
                spawn.z + roll(MINION_ROW_MIN..MINION_ROW_MAX)
            }
        return List(3) { CoordGrid(column, startRow + it, spawn.level) }
    }

    private fun tiles(
        spawn: CoordGrid,
        element: TitanElement,
        south: Int,
        north: Int,
    ): List<CoordGrid> {
        val level = spawn.level
        val xs = columns(spawn, element)
        return buildList(xs.count() * (north - south + 1)) {
            for (x in xs) {
                for (z in south..north) {
                    add(CoordGrid(x, z, level))
                }
            }
        }
    }
}
