package org.rsmod.content.travel.maptransports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class MapTransportsTest {
    private val transports =
        """
        # Origin	Destination	menuOption menuTarget objectID	Skills	Items	Quests	Varbits	VarPlayers	Duration	Display info
        # HAM Hideout
        3165 3252 0	3149 9652 0	Climb-down Trapdoor loc.trapdoor						1
        3149 9652 0	3165 3251 0	Climb-up Ladder loc.ladder						2
        3150 9653 0	3165 3251 0	Climb-up Ladder loc.ladder						2

        3055 3245 0	3055 3242 1	Cross Gangplank loc.gangplank						3
        """.trimIndent()

    private val shortcuts =
        """
        # Origin	Destination	menuOption menuTarget objectID	Skills	Items	Varbits	VarPlayers	Duration	Quests
        2220 3155 0	2220 3152 0	Step-over Tripwire loc.tripwire	1 stat.agility				8
        2932 3355 0	2936 3355 0	Climb-over Crumbling wall loc.crumbling_wall	5 stat.agility;10 stat.strength;1 HEADSLOT				2
        """.trimIndent()

    @Test
    fun `rows read their tiles, option and loc`() {
        val rows = MapTransports.parse(transports)
        assertEquals(4, rows.size)
        val trapdoor = rows.first()
        assertEquals(CoordGrid(3165, 3252, 0), trapdoor.origin)
        assertEquals(CoordGrid(3149, 9652, 0), trapdoor.destination)
        assertEquals("Climb-down", trapdoor.option)
        assertEquals("loc.trapdoor", trapdoor.loc)
        assertEquals(1, trapdoor.duration)
    }

    @Test
    fun `columns are found by name in each file's own order`() {
        val rows = MapTransports.parse(shortcuts)
        assertEquals(2, rows.size)
        assertEquals(8, rows[0].duration)
        assertEquals(listOf("stat.agility" to 1), rows[0].skills)
    }

    @Test
    fun `several skills are read and equipment slots are ignored`() {
        val wall = MapTransports.parse(shortcuts)[1]
        assertEquals(listOf("stat.agility" to 5, "stat.strength" to 10), wall.skills)
    }

    @Test
    fun `the row starting nearest the player is chosen`() {
        val ladders = MapTransports.parse(transports).filter { it.loc == "loc.ladder" }
        assertEquals(CoordGrid(3150, 9653, 0), MapTransports.pick(ladders, CoordGrid(3151, 9654, 0))!!.origin)
        assertEquals(CoordGrid(3149, 9652, 0), MapTransports.pick(ladders, CoordGrid(3148, 9651, 0))!!.origin)
    }

    @Test
    fun `a row on the player's floor beats a nearer one on another floor`() {
        val rows =
            listOf(
                MapTransport(CoordGrid(3200, 3200, 1), CoordGrid(1, 1, 0), "Climb-down", "loc.ladder", emptyList(), 1),
                MapTransport(CoordGrid(3205, 3200, 0), CoordGrid(2, 2, 0), "Climb-down", "loc.ladder", emptyList(), 1),
            )
        assertEquals(CoordGrid(2, 2, 0), MapTransports.pick(rows, CoordGrid(3200, 3200, 0))!!.destination)
    }

    @Test
    fun `comments, blanks and broken rows are skipped`() {
        val text = "# Origin\tDestination\tmenuOption menuTarget objectID\n\n3 3\t1 1 0\tOpen Door loc.door\n# note\n"
        assertTrue(MapTransports.parse(text).isEmpty())
    }
}
