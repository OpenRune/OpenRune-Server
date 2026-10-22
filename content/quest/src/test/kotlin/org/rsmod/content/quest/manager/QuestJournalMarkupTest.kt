package org.rsmod.content.quest.manager

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class QuestJournalMarkupTest {
    @Test
    fun `highlights switch colour without closing tags`() {
        val lines = journalLines("<blue>across a <red>Giant</red> who didn't seem willing to fight.</blue>")
        assertEquals(
            listOf("<col=000080>across a <col=800000>Giant<col=000080> who didn't seem willing to fight."),
            lines,
        )
    }

    @Test
    fun `wrapped line restates the base colour before a leading highlight`() {
        val lines =
            journalLines(
                "<blue>With the <red>Foundry</red> ready to go, <red>Kovac</red> has asked me to " +
                    "help him <red>Foundry</red> swords.</blue>",
            )
        assertEquals(
            listOf(
                "<col=000080>With the <col=800000>Foundry<col=000080> ready to go, " +
                    "<col=800000>Kovac<col=000080> has asked me to help him",
                "<col=000080><col=800000>Foundry<col=000080> swords.",
            ),
            lines,
        )
    }

    @Test
    fun `struck lines drop colours and keep a single leading str`() {
        val lines = journalLines("<black><str>I found the <red>Giants' Plateau</red>.</str></black>")
        assertEquals(listOf("<str>I found the Giants' Plateau."), lines)
    }

    @Test
    fun `tag-only and empty lines are blank`() {
        assertEquals(listOf("", ""), journalLines("<blue></blue>\n"))
    }

    @Test
    fun `raw colour markup passes through unchanged`() {
        val line = "rewarded with <col=800000>uncut gems<col=000080>, <col=800000>1 Quest Point<col=000080>."
        assertEquals(listOf(line), journalLines(line))
        assertEquals(listOf("<col=ff0000>QUEST COMPLETE!"), journalLines("<col=FF0000>QUEST COMPLETE!"))
    }
}
