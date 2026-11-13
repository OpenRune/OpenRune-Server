package org.rsmod.content.minigames.sepulchre

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
class SepulchreStrangeTilesTest {
    private fun GameTestState.runTilesTest(body: GameTestScope.() -> Unit) =
        runGameTest(SepulchreStrangeTileScript::class) {
            advance()
            body()
        }

    private fun GameTestScope.lightEveryTile() {
        repeat(FLOOR_FIVE_TILES) { random.next = 0 }
    }

    @Test
    fun GameTestState.`a player on a lit tile is sent along the path a tick later`() =
        runTilesTest {
            allocZoneCollision(LANDING)
            onFloor(5, TILE, remaining = 200)
            lightEveryTile()

            advance()
            assertEquals(TILE, player.coords)
            advance()
            assertEquals(LANDING, player.coords)
        }

    @Test
    fun GameTestState.`a player who just landed is not sent again by the lit tile under them`() =
        runTilesTest {
            allocZoneCollision(LANDING)
            allocZoneCollision(BEYOND)
            onFloor(5, TILE, remaining = 200)
            lightEveryTile()

            val path = mutableListOf<CoordGrid>()
            repeat(4) {
                advance()
                path += player.coords
            }

            assertEquals(listOf(TILE, LANDING, LANDING, LANDING), path)
        }

    @Test
    fun GameTestState.`nobody is sent off a floor they are not timed on`() = runTilesTest {
        allocZoneCollision(LANDING)
        startAt(TILE)
        lightEveryTile()

        advance(3)

        assertEquals(TILE, player.coords)
    }

    private companion object {
        const val FLOOR_FIVE_TILES = 96
        val TILE = CoordGrid(2243, 5837, 1)
        val LANDING = CoordGrid(2243, 5843, 1)
        val BEYOND = CoordGrid(2243, 5849, 1)
    }
}
