package org.rsmod.content.skills.agility

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.api.hit.plugin.PlayerHitScript
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.map.CoordGrid

class RooftopCoursesTest {
    private class Course(val start: CoordGrid, val lap: List<String>)

    @Test
    fun GameTestState.`below the course level the obstacle refuses`() = runAgilityTest {
        setAgility(19)
        startAt(AL_KHARID.start)
        advance()

        val messages = assertRefused(AL_KHARID.lap.first())

        assertEquals(listOf("You need an Agility level of 20 to attempt this course."), messages)
    }

    @Test
    fun GameTestState.`a failed tightrope drops the player and hurts`() =
        runAgilityTest(PlayerHitScript::class) {
            setAgility(20)
            setHitpoints(10)
            startAt(CoordGrid(3272, 3182, 3))
            advance()
            val before = agilityXp()
            val hitpoints = player.hitpoints

            random.next = 100
            random.then = 3
            player.opLoc1(locNear("loc.rooftops_kharid_tightrope_1", player.coords))
            advance(ticks = 6)

            assertEquals(CoordGrid(3272, 3177, 0), player.coords)
            assertEquals(before, agilityXp())
            assertEquals(hitpoints - 3, player.hitpoints)
        }

    @Test
    fun GameTestState.`a rellekka fall takes a tenth of the hitpoints left`() =
        runAgilityTest(PlayerHitScript::class) {
            setAgility(80)
            setHitpoints(50)
            startAt(CoordGrid(2622, 3658, 3))
            advance()

            random.next = 100
            random.then = 0
            player.opLoc1(locNear("loc.rooftops_rellekka_tightrope_1", player.coords))
            advance(ticks = 6)

            assertEquals(CoordGrid(2624, 3656, 0), player.coords)
            assertEquals(45, player.hitpoints)
        }

    @Test
    fun GameTestState.`a fall breaks the lap`() = runAgilityTest {
        setAgility(20)
        blockMarks()
        startAt(AL_KHARID.start)
        advance()
        cross(AL_KHARID.lap.first())
        random.next = 100
        random.then = 1
        player.opLoc1(locNear(AL_KHARID.lap[1], player.coords))
        advance(ticks = 6)

        setAgility(99)
        startAt(CoordGrid(3266, 3166, 3))
        val messages = crossAll(AL_KHARID.lap.drop(2))

        assertTrue(messages.none { "lap count" in it })
    }

    @Test
    fun GameTestState.`hard desert diary raises the drying line xp`() = runAgilityTest {
        setAgility(70)
        startAt(CoordGrid(3362, 3002, 2))
        advance()
        val plain = agilityXp()
        cross("loc.rooftops_pollnivneach_line")
        assertEquals(plain + 5400, agilityXp())

        setVarbit("varbit.desert_diary_hard_complete", 1)
        startAt(CoordGrid(3362, 3002, 2))
        advance()
        val diary = agilityXp()
        cross("loc.rooftops_pollnivneach_line")
        assertEquals(diary + 6660, agilityXp())
    }

    private fun GameTestScope.runLapRolling(course: Course, vararg rolls: Int) {
        setAgility(99)
        startAt(course.start)
        advance()
        crossAll(course.lap.dropLast(1))
        for (roll in rolls) {
            random.next = roll
        }
        cross(course.lap.last())
    }

    @Test
    fun GameTestState.`kandarin diary rerolls a missed seers mark`() = runAgilityTest { deps ->
        runLapRolling(SEERS, 29)
        assertEquals(emptyList<Any>(), marksOn(deps, markTiles(SEERS_ROW)))

        setVarbit("varbit.kandarin_diary_hard_complete", 1)
        setMarkReady(0)
        runLapRolling(SEERS, 29, 14, 0)
        assertEquals(1, marksOn(deps, markTiles(SEERS_ROW)).size)
    }

    @Test
    fun GameTestState.`elite ardougne diary can shorten the timer`() = runAgilityTest { deps ->
        setVarbit("varbit.ardougne_diary_elite_complete", 1)
        val before = currentMinute()

        runLapRolling(ARDOUGNE, 0, 0, 0)

        assertEquals(1, marksOn(deps, markTiles(ARDOUGNE_ROW)).size)
        assertTrue(markReady() in before + 2..currentMinute() + 2)
    }

    @Test
    fun GameTestState.`canifis marks ignore the level penalty`() = runAgilityTest { deps ->
        runLapRolling(CANIFIS, 9, 0)

        assertEquals(1, marksOn(deps, markTiles(CANIFIS_ROW)).size)
    }

    private fun GameTestScope.setHitpoints(level: Int) {
        player.statMap.setBaseLevel("stat.hitpoints", level.toByte())
        player.statMap.setCurrentLevel("stat.hitpoints", level.toByte())
    }

    private fun markTiles(row: String): List<CoordGrid> = AgilityCoursesRow.getRow(row).markTiles

    private companion object {
        const val SEERS_ROW = "dbrow.agility_course_seers"
        const val ARDOUGNE_ROW = "dbrow.agility_course_ardougne"
        const val CANIFIS_ROW = "dbrow.agility_course_canifis"

        val AL_KHARID =
            Course(
                start = CoordGrid(3273, 3195, 0),
                lap =
                    listOf(
                        "loc.rooftops_kharid_wallclimb",
                        "loc.rooftops_kharid_tightrope_1",
                        "loc.rooftops_kharid_rope_swing",
                        "loc.rooftops_kharid_slide_side",
                        "loc.rooftops_kharid_bamboo_tree_top",
                        "loc.rooftops_kharid_wallclimb_2",
                        "loc.rooftops_kharid_tightrope_4",
                        "loc.rooftops_kharid_leapdown",
                    ),
            )

        val CANIFIS =
            Course(
                start = CoordGrid(3508, 3488, 0),
                lap =
                    listOf(
                        "loc.rooftops_canifis_start_tree",
                        "loc.rooftops_canifis_jump",
                        "loc.rooftops_canifis_jump_2",
                        "loc.rooftops_canifis_jump_5",
                        "loc.rooftops_canifis_jump_3",
                        "loc.rooftops_canifis_polevault",
                        "loc.rooftops_canifis_jump_4",
                        "loc.rooftops_canifis_leapdown",
                    ),
            )

        val SEERS =
            Course(
                start = CoordGrid(2729, 3489, 0),
                lap =
                    listOf(
                        "loc.rooftops_seers_wallclimb",
                        "loc.rooftops_seers_jump",
                        "loc.rooftops_seers_tightrope",
                        "loc.rooftops_seers_jump_1",
                        "loc.rooftops_seers_jump_2",
                        "loc.rooftops_seers_leapdown",
                    ),
            )

        val ARDOUGNE =
            Course(
                start = CoordGrid(2673, 3298, 0),
                lap =
                    listOf(
                        "loc.rooftops_ardy_wallclimb",
                        "loc.rooftops_ardy_jump",
                        "loc.rooftops_ardy_plank",
                        "loc.rooftops_ardy_jump_2",
                        "loc.rooftops_ardy_jump_3",
                        "loc.rooftops_ardy_wallcrossing",
                        "loc.rooftops_ardy_jump_4",
                    ),
            )
    }
}
