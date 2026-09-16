package org.rsmod.content.areas.city.draynor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.game.loc.BoundLocInfo
import org.rsmod.game.loc.LocAngle
import org.rsmod.game.loc.LocEntity
import org.rsmod.game.loc.LocShape
import org.rsmod.map.CoordGrid

class ManorDoorCrossingTest {
    @Test
    fun `wall doors cross the same edge in both directions for every rotation`() {
        val offsets =
            mapOf(
                LocAngle.West to (-1 to 0),
                LocAngle.North to (0 to 1),
                LocAngle.East to (1 to 0),
                LocAngle.South to (0 to -1),
            )
        for ((angle, offset) in offsets) {
            val door = door(LocShape.WallStraight, angle)
            val across = door.coords.translate(offset.first, offset.second)
            assertEquals(across, door.acrossTile())
            assertEquals(listOf(across), crossingRoute(door.coords, door))
            assertEquals(listOf(door.coords), crossingRoute(across, door))
        }
    }

    @Test
    fun `centrepiece gates cross their occupied tile from either side`() {
        for (shape in listOf(LocShape.CentrepieceStraight, LocShape.CentrepieceDiagonal)) {
            for (angle in LocAngle.entries) {
                val door = door(shape, angle)
                assertEquals(
                    listOf(door.coords, door.acrossTile()),
                    crossingRoute(door.behindTile(), door),
                )
                assertEquals(
                    listOf(door.coords, door.behindTile()),
                    crossingRoute(door.acrossTile(), door),
                )
                assertEquals(2, crossingTiles(door.behindTile(), crossingRoute(door.behindTile(), door)))
            }
        }
    }

    @Test
    fun `standing in a doorway does not add an occupied leading waypoint`() {
        val door = door(LocShape.CentrepieceStraight, LocAngle.West)
        val route = crossingRoute(door.coords, door)
        assertEquals(listOf(door.acrossTile()), route)
        assertEquals(1, crossingTiles(door.coords, route))
    }

    @Test
    fun `bookcase open duration covers both orthogonal legs and the final arrival`() {
        val start = CoordGrid(3095, 3360)
        val route = listOf(CoordGrid(3096, 3358), CoordGrid(3098, 3358))
        assertEquals(4, crossingTiles(start, route))
        assertEquals(6, crossingOpenTicks(crossingTiles(start, route)))
    }

    @Test
    fun `short crossings retain the minimum open window`() {
        for (tiles in 0..2) {
            assertEquals(4, crossingOpenTicks(tiles))
        }
        assertEquals(5, crossingOpenTicks(3))
        val tile = CoordGrid(3098, 3358)
        assertEquals(1, crossingTiles(tile, listOf(tile)))
    }

    private fun door(shape: LocShape, angle: LocAngle): BoundLocInfo =
        BoundLocInfo(
            coords = CoordGrid(3104, 9765, 1),
            entity = LocEntity(id = 0, shape = shape.id, angle = angle.id),
            layer = if (shape == LocShape.WallStraight) 0 else 2,
            width = 1,
            length = 1,
            forceApproachFlags = 0,
        )
}
