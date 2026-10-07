package org.rsmod.content.generic.locs.stile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.loc.LocAngle
import org.rsmod.map.CoordGrid

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class FenceCrossingScriptTest {
    @Test
    fun GameTestState.`climbing a north-facing fence from the south lands north of it`() =
        runGameTest(FenceCrossingScript::class) {
            val fence = placeMapLoc(FENCE, BROKEN_FENCE, angle = LocAngle.North)
            player.teleport(FENCE.translateZ(-1))
            player.opLoc1(fence)
            advance(ticks = 5)
            assertEquals(FENCE.translateZ(1), player.coords)
        }

    @Test
    fun GameTestState.`climbing a north-facing fence from the north lands south of it`() =
        runGameTest(FenceCrossingScript::class) {
            val fence = placeMapLoc(FENCE, BROKEN_FENCE, angle = LocAngle.North)
            player.teleport(FENCE.translateZ(1))
            player.opLoc1(fence)
            advance(ticks = 5)
            assertEquals(FENCE.translateZ(-1), player.coords)
        }

    @Test
    fun GameTestState.`climbing a west-facing fence from the west lands east of it`() =
        runGameTest(FenceCrossingScript::class) {
            val fence = placeMapLoc(FENCE, BROKEN_FENCE, angle = LocAngle.West)
            player.teleport(FENCE.translateX(-1))
            player.opLoc1(fence)
            advance(ticks = 5)
            assertEquals(FENCE.translateX(1), player.coords)
        }

    @Test
    fun GameTestState.`climbing a west-facing fence from the east lands west of it`() =
        runGameTest(FenceCrossingScript::class) {
            val fence = placeMapLoc(FENCE, BROKEN_FENCE, angle = LocAngle.West)
            player.teleport(FENCE.translateX(1))
            player.opLoc1(fence)
            advance(ticks = 5)
            assertEquals(FENCE.translateX(-1), player.coords)
        }

    private companion object {
        const val BROKEN_FENCE = "loc.gertrudefence"
        val FENCE = CoordGrid(0, 51, 54, 44, 36)
    }
}
