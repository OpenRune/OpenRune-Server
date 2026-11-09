package org.rsmod.content.quest.manager

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class QuestJournalMarkupTest {
    @Test
    fun `wraps by pixel width not character count`() {
        val lines =
            journalLines(
                "<black><str>While exploring the Giants' Plateau east of Al Kharid, I came across a Giant " +
                    "who didn't seem willing to fight. He told me that his name was Kovac and that he " +
                    "needed my help. He asked me to join him in a nearby Cave.</str></black>",
            )
        assertEquals(
            listOf(
                "<str>While exploring the Giants' Plateau east of Al Kharid, I came",
                "<str>across a Giant who didn't seem willing to fight. He told me that",
                "<str>his name was Kovac and that he needed my help. He asked me",
                "<str>to join him in a nearby Cave.",
            ),
            lines,
        )
    }

    @Test
    fun `coloured paragraph wraps by pixel width and keeps highlights`() {
        val lines =
            journalLines(
                "<blue>I followed <red>Kovac</red> into the <red>Cave</red> and found that it led to an " +
                    "<red>Ancient Foundry</red>. <red>Kovac</red> told me that it was built by his " +
                    "ancestors, and that he wants to get it running again. I need to help him by " +
                    "repairing three tools.</blue>",
            )
        assertEquals(
            listOf(
                "<col=000080>I followed <col=800000>Kovac<col=000080> into the <col=800000>Cave<col=000080> " +
                    "and found that it led to an",
                "<col=000080><col=800000>Ancient Foundry<col=000080>. <col=800000>Kovac<col=000080> told me " +
                    "that it was built by his",
                "<col=000080>ancestors, and that he wants to get it running again. I need to",
                "<col=000080>help him by repairing three tools.",
            ),
            lines,
        )
    }

    @Test
    fun `a line of exactly 350 pixels stays whole and the next word wraps`() {
        val fits = "had a book on a shelf in his house. Cassius might want to read"
        assertEquals(listOf(fits, "the"), journalLines("$fits the"))
    }

    @Test
    fun `a line that would reach 351 pixels wraps`() {
        val fits = "Having returned Seth's book to Cassius, he then offered me"
        assertEquals(listOf(fits, "a sample"), journalLines("$fits a sample"))
    }

    @Test
    fun `escaped angle brackets are measured as the characters they stand for`() {
        val escaped = journalLines("<lt> ".repeat(60))
        assertEquals(journalLines("< ".repeat(60)).size, escaped.size)
        assertTrue(escaped.size > 1)
        assertEquals(listOf("<lt>"), journalLines("<lt>"))
    }

    @Test
    fun `a word wider than the line is left unbroken`() {
        val word = "W".repeat(60)
        assertEquals(listOf(word), journalLines(word))
        assertEquals(listOf(word, "end"), journalLines("$word end"))
    }

    @Test
    fun `explicit breaks start new lines`() {
        assertEquals(listOf("one", "two"), journalLines("one<br>two"))
        assertEquals(listOf("one", "", "two"), journalLines("one\n\ntwo"))
    }

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

    companion object {
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
        }
    }
}
