package org.rsmod.content.skills.agility.pack.shortcut

import dev.openrune.definition.dbtables.DBTableBuilder
import kotlin.math.abs
import org.rsmod.content.skills.agility.pack.AgilityObstaclesTable.REQUIREMENT_NOT_CARRIED
import org.rsmod.content.skills.agility.pack.LaneBuilder
import org.rsmod.content.skills.agility.pack.lane
import org.rsmod.content.skills.agility.pack.lanes
import org.rsmod.content.skills.agility.pack.requirement
import org.rsmod.content.skills.agility.pack.tile
import org.rsmod.map.CoordGrid

private const val CAVE_OBSTACLE = "loc.fossil_underwater_cave_obstacle1"
private const val SHIP_OBSTACLE = "loc.fossil_underwater_cave_obstacle2"
private const val HOLE = "loc.fossil_underwater_cave_obstacle3"
private const val TUNNEL = "loc.fossil_underwater_deepwater_agihole"
private const val PLANT_DOOR = "loc.fossil_underwater_deepwater_hole"
private const val FISHBOWL = "obj.hundred_pirate_diving_helmet"

private const val CAVE_WALL = "seq.human_swim_cavewall"
private const val SHIP_WALL = "seq.human_swim_shipwall"
private const val HOLE_ENTER = "seq.human_swim_hole_enter"
private const val HOLE_LEAVE = "seq.human_swim_hole_leave"

private const val UNDERWATER_XP = 45
private const val GLIDE_PER_TILE = 30

private val CAVE_WALLS_EAST_WEST =
    listOf(
        tile(3726, 10245, 1),
        tile(3758, 10260, 1),
        tile(3760, 10277, 1),
        tile(3763, 10244, 1),
        tile(3771, 10275, 1),
        tile(3824, 10264, 1),
    )

private val SHIP_WALLS_EAST_WEST =
    listOf(
        tile(3755, 10268, 1),
        tile(3764, 10269, 1),
        tile(3770, 10259, 1),
        tile(3775, 10270, 1),
        tile(3808, 10253, 1),
    )

private val SHIP_WALLS_NORTH_SOUTH = listOf(tile(3754, 10244, 1))

private val TUNNEL_NORTH_SOUTH = listOf(tile(3764, 10281, 1))

private val HOLE_PAIRS =
    listOf(
        tile(3716, 10243, 1) to tile(3779, 10277, 1),
        tile(3754, 10241, 1) to tile(3779, 10241, 1),
        tile(3772, 10267, 1) to tile(3823, 10247, 1),
        tile(3772, 10280, 1) to tile(3816, 10271, 1),
        tile(3833, 10289, 1) to tile(3789, 10298, 1),
    )

/*
 * Fossil Island's Underwater Agility and Thieving: the cave and wreck walls, the tunnel and
 * Mairin's plant door are glided through, the holes swum between in pairs. None can fail and each
 * pays 4.5 xp. Every crossing is captured but the way back through the last hole pair. A fishbowl
 * helmet is turned back before the deeper water.
 */
internal fun DBTableBuilder.fossilUnderwaterObstacles() {
    row("dbrow.agility_shortcut_fossil_underwater_walls") {
        shortcut(level = 1, xp = UNDERWATER_XP, CAVE_OBSTACLE, SHIP_OBSTACLE, TUNNEL, PLANT_DOOR)
        requirement(REQUIREMENT_NOT_CARRIED, FISHBOWL)
        val crossings =
            CAVE_WALLS_EAST_WEST.flatMap(::eastWest).map { it to CAVE_WALL } +
                SHIP_WALLS_EAST_WEST.flatMap(::eastWest).map { it to SHIP_WALL } +
                SHIP_WALLS_NORTH_SOUTH.flatMap(::northSouth).map { it to SHIP_WALL } +
                TUNNEL_NORTH_SOUTH.flatMap(::northSouth).map { it to CAVE_WALL } +
                plantDoor().map { it to CAVE_WALL }
        val lanes = crossings.map { (way, seq) -> lane(way.from) { swimThrough(way, seq) } }
        lanes(*lanes.toTypedArray())
    }

    row("dbrow.agility_shortcut_fossil_underwater_holes") {
        shortcut(level = 1, xp = UNDERWATER_XP, HOLE)
        // UNVERIFIED: only the way from (3833, 10289) is captured on the last pair.
        ways(HOLE_PAIRS.flatMap { (a, b) -> both(a, b) }) { swimHole(it) }
    }
}

private fun eastWest(loc: CoordGrid): List<Way> = both(loc.translateX(-1), loc.translateX(1))

private fun northSouth(loc: CoordGrid): List<Way> = both(loc.translateZ(-1), loc.translateZ(1))

/** The door's wall stands on its west tile's east edge, so the way out glides a single tile. */
private fun plantDoor(): List<Way> =
    listOf(
        way(tile(3768, 10288, 1), tile(3766, 10288, 1)),
        way(tile(3767, 10288, 1), tile(3768, 10288, 1)),
    )

/** Captured: a glide of 30 client cycles a tile, paid the tick after it ends. */
private fun LaneBuilder.swimThrough(way: Way, seq: String) {
    val tiles = maxOf(abs(way.to.x - way.from.x), abs(way.to.z - way.from.z))
    exactMove(way.to, delay1 = 0, delay2 = GLIDE_PER_TILE * tiles, angleTo(way.from, way.to))
    anim(seq)
    next(tiles + 1)
    land()
}

/** Captured: the player is stepped onto the hole, dives the tick after and surfaces two later. */
private fun LaneBuilder.swimHole(way: Way) {
    anim(HOLE_ENTER)
    next(2)
    telejump(way.to)
    anim(HOLE_LEAVE)
    land()
}
