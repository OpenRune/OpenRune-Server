package org.rsmod.tools.mcp.wiki

import kotlin.test.Test
import kotlin.test.assertEquals

class WikiInfoboxNpcIdsTest {
    @Test
    fun `parses versioned id lines from infobox monster`() {
        val snippet =
            """
            {{Infobox Monster
            |name = Man
            |id1 = 3106,6818,6987
            |id2 = 3107,6988
            |id10 = 3014
            |id9 = 3652
            }}
            """.trimIndent()

        val lines = WikiInfoboxNpcIds.parseMonsterIdLines(snippet)
        assertEquals(listOf(1, 2, 9, 10), lines.map { it.label.removePrefix("id").toInt() })
        assertEquals(listOf(3106, 6818, 6987), lines.first { it.label == "id1" }.npcIds)
        assertEquals(listOf(3107, 6988), lines.first { it.label == "id2" }.npcIds)
        assertEquals(listOf(3652), lines.first { it.label == "id9" }.npcIds)
        assertEquals(listOf(3014), lines.first { it.label == "id10" }.npcIds)
    }

    @Test
    fun `parses bare id line when no versioned ids`() {
        val snippet =
            """
            {{Infobox NPC
            |name = Rat
            |id = 47
            }}
            """.trimIndent()

        val lines = WikiInfoboxNpcIds.parseMonsterIdLines(snippet)
        assertEquals(1, lines.size)
        assertEquals("id", lines.single().label)
        assertEquals(listOf(47), lines.single().npcIds)
    }

    @Test
    fun `versioned ids take precedence over bare id`() {
        val snippet =
            """
            |id1 = 1,2
            |id = 99
            """.trimIndent()

        val lines = WikiInfoboxNpcIds.parseMonsterIdLines(snippet)
        assertEquals(1, lines.size)
        assertEquals("id1", lines.single().label)
    }
}
