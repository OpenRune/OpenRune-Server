package org.rsmod.content.generic.locs.stile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

class StileScriptTest {
    @Test
    fun GameTestState.`climbing from the south lands north of the stile`() =
        runGameTest(StileScript::class) {
            val stile = placeMapLoc(SOUTH_TILE, STILE, angle = LocAngle.West)
            player.teleport(SOUTH_TILE.translateZ(-1))
            player.opLoc1(stile)
            advance(ticks = CROSSING_TICKS)
            assertEquals(NORTH_TILE.translateZ(1), player.coords)
        }

    @Test
    fun GameTestState.`climbing from the north lands south of the stile`() =
        runGameTest(StileScript::class) {
            val stile = placeMapLoc(SOUTH_TILE, STILE, angle = LocAngle.West)
            player.teleport(NORTH_TILE.translateZ(1))
            player.opLoc1(stile)
            advance(ticks = CROSSING_TICKS)
            assertEquals(SOUTH_TILE.translateZ(-1), player.coords)
        }

    @Test
    fun GameTestState.`climbing from beside the stile along the fence still crosses it`() =
        runGameTest(StileScript::class) {
            val stile = placeMapLoc(SOUTH_TILE, STILE, angle = LocAngle.West)
            player.teleport(NORTH_TILE.translateX(-1))
            player.opLoc1(stile)
            advance(ticks = CROSSING_TICKS)
            assertEquals(SOUTH_TILE.translateZ(-1), player.coords)
        }

    @Test
    fun GameTestState.`climbing from a few tiles away walks up and crosses`() =
        runGameTest(StileScript::class) {
            val stile = placeMapLoc(SOUTH_TILE, STILE, angle = LocAngle.West)
            player.teleport(SOUTH_TILE.translate(-1, -4))
            player.opLoc1(stile)
            advance(ticks = CROSSING_TICKS + 4)
            assertEquals(NORTH_TILE.translateZ(1), player.coords)
        }

    @Test
    fun GameTestState.`a north-facing stile is crossed east to west`() =
        runGameTest(StileScript::class) {
            val stile = placeMapLoc(SOUTH_TILE, STILE, angle = LocAngle.North)
            player.teleport(SOUTH_TILE.translateX(2))
            player.opLoc1(stile)
            advance(ticks = CROSSING_TICKS)
            assertEquals(SOUTH_TILE.translateX(-1), player.coords)
        }

    private companion object {
        const val STILE = "loc.qip_sheep_shearer_fullstyle"
        const val CROSSING_TICKS = 10
        val SOUTH_TILE = CoordGrid(0, 51, 54, 44, 36)
        val NORTH_TILE = SOUTH_TILE.translateZ(1)
    }
}
