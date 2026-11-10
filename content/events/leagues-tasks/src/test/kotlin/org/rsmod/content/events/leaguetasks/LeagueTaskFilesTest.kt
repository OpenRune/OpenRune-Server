package org.rsmod.content.events.leaguetasks

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class LeagueTaskFilesTest {
    private fun source(path: String, text: String) = LeagueTaskSource("league_tasks/$path", text)

    private val leagueA = source("league_a/league.toml", "type = 6")
    private val leagueB = source("league_b/league.toml", "type = 7")

    private val generic =
        source(
            "generic/mining.toml",
            """
            [[task_mine]]
            trigger = "product"
            stat = "stat.mining"
            obj = ["obj.tin_ore", "obj.copper_ore"]
            times = 5
            counter = "varbit.mined"

            [[task_two_parts]]
            trigger = "kill"
            npc = "npc.a"
            counter = "varbit.a"

            [[task_two_parts]]
            trigger = "special_attack"
            counter = "varbit.b"
            """
                .trimIndent(),
        )

    private val leagueOnly =
        source(
            "league_a/areas.toml",
            """
            [[task_enter]]
            trigger = "enter"
            area = "area.somewhere"
            """
                .trimIndent(),
        )

    @Test
    fun `a league sees generic tasks plus its own`() {
        val files = LeagueTaskFiles.parse(listOf(leagueA, leagueB, generic, leagueOnly))

        assertEquals(setOf("task_mine", "task_two_parts", "task_enter"), files.definitions("league_a").keys)
        assertEquals(setOf("task_mine", "task_two_parts"), files.definitions("league_b").keys)
        assertEquals(6, files.leagues.getValue("league_a").type)
    }

    @Test
    fun `values parse with defaults and single values as lists`() {
        val files = LeagueTaskFiles.parse(listOf(leagueA, generic))
        val mine = files.generic.getValue("task_mine").requirements.single()
        val parts = files.generic.getValue("task_two_parts").requirements

        assertEquals(listOf("obj.tin_ore", "obj.copper_ore"), mine.obj)
        assertEquals(listOf("stat.mining"), mine.stat)
        assertEquals(5, mine.times)
        assertEquals(listOf("kill", "special_attack"), parts.map { it.trigger })
        assertEquals(listOf("npc.a"), parts[0].npc)
        assertEquals(1, parts[1].times)
        assertEquals(0, parts[1].value)
    }

    @Test
    fun `a task in both generic and a league folder fails`() {
        val clash = source("league_a/mining.toml", "[[task_mine]]\ntrigger = \"product\"")

        val error = assertThrows<IllegalStateException> {
            LeagueTaskFiles.parse(listOf(leagueA, generic, clash))
        }
        assertTrue("generic/mining.toml" in error.message!!)
        assertTrue("league_a/mining.toml" in error.message!!)
    }

    @Test
    fun `a task in two files of one folder fails`() {
        val twice = source("generic/other.toml", "[[task_mine]]\ntrigger = \"product\"")

        assertThrows<IllegalStateException> { LeagueTaskFiles.parse(listOf(generic, twice)) }
    }

    @Test
    fun `a league folder without league toml fails`() {
        assertThrows<IllegalStateException> { LeagueTaskFiles.parse(listOf(leagueOnly)) }
    }

    @Test
    fun `an unknown key fails with the file named`() {
        val typo = source("generic/typo.toml", "[[task_x]]\ntrigger = \"kill\"\nnpcs = \"npc.a\"")

        val error = assertThrows<IllegalStateException> { LeagueTaskFiles.parse(listOf(typo)) }
        assertTrue("generic/typo.toml" in error.message!!)
    }

    @Test
    fun `two leagues sharing a type fail`() {
        val sameType = source("league_b/league.toml", "type = 6")

        assertThrows<IllegalStateException> { LeagueTaskFiles.parse(listOf(leagueA, sameType)) }
    }
}
