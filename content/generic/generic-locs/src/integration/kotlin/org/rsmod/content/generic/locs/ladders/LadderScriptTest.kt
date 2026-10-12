package org.rsmod.content.generic.locs.ladders

import dev.openrune.types.ObjectServerType
import org.junit.jupiter.api.Test
import org.rsmod.api.config.refs.params
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.map.CoordGrid

class LadderScriptTest {
    @Test
    fun GameTestState.`climbing up a ladder moves the player up one level`() =
        runGameTest(LadderScript::class) {
            // given
            val ladder = placeMapLoc(GROUND_LADDER, findLadder("content.ladder_up"))
            player.teleport(GROUND_FOOT)
            allocZoneCollision(UPPER_FOOT)

            // when
            player.opLoc1(ladder)
            advance(ticks = CLIMB_TICKS)

            // then
            assertEquals(UPPER_FOOT, player.coords)
        }

    @Test
    fun GameTestState.`climbing down a ladder moves the player down one level`() =
        runGameTest(LadderScript::class) {
            // given
            val ladder = placeMapLoc(UPPER_LADDER, findLadder("content.ladder_down"))
            player.teleport(UPPER_FOOT)
            allocZoneCollision(GROUND_FOOT)

            // when
            player.opLoc1(ladder)
            advance(ticks = CLIMB_TICKS)

            // then
            assertEquals(GROUND_FOOT, player.coords)
        }

    @Test
    fun GameTestState.`climb-up on a two-way ladder moves the player up one level`() =
        runGameTest(LadderScript::class) {
            // given
            val ladder = placeMapLoc(UPPER_LADDER, findLadder("content.ladder_option"))
            player.teleport(UPPER_FOOT)
            allocZoneCollision(TOP_FOOT)

            // when
            player.opLoc2(ladder)
            advance(ticks = CLIMB_TICKS)

            // then
            assertEquals(TOP_FOOT, player.coords)
        }

    @Test
    fun GameTestState.`climb-down on a two-way ladder moves the player down one level`() =
        runGameTest(LadderScript::class) {
            // given
            val ladder = placeMapLoc(UPPER_LADDER, findLadder("content.ladder_option"))
            player.teleport(UPPER_FOOT)
            allocZoneCollision(GROUND_FOOT)

            // when
            player.opLoc3(ladder)
            advance(ticks = CLIMB_TICKS)

            // then
            assertEquals(GROUND_FOOT, player.coords)
        }

    private fun GameTestScope.findLadder(content: String): ObjectServerType =
        findLocType(content) { !it.hasParam(params.translate_level) }

    private companion object {
        const val CLIMB_TICKS = 5
        val GROUND_LADDER = CoordGrid(0, 50, 50, 34, 31)
        val UPPER_LADDER = GROUND_LADDER.translateLevel(1)
        val GROUND_FOOT = GROUND_LADDER.translateZ(1)
        val UPPER_FOOT = GROUND_FOOT.translateLevel(1)
        val TOP_FOOT = GROUND_FOOT.translateLevel(2)
    }
}
