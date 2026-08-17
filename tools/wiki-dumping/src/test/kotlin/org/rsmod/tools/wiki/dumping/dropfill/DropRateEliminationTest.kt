package org.rsmod.tools.wiki.dumping.dropfill

import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DropRateEliminationTest {
    private val file = Path.of("ArmouredKrakenDropTable.kt")

    private fun anchor(item: String, line: Int, rarity: String = "1/{{#expr:1 round 1}}") =
        DropAnchor(
            file = file,
            line = line,
            item = item,
            table = "main",
            subsection = null,
            rarity = rarity,
            page = "Armoured_kraken",
        )

    private fun row(
        rarity: String,
        qtyLow: Int = 1,
        qtyHigh: Int = qtyLow,
        item: String = "Snape grass seed",
    ) = DropsRow(item = item, rarity = rarity, qtyLow = qtyLow, qtyHigh = qtyHigh, noted = false)

    private fun filled(anchor: DropAnchor, rarity: String, qtyLow: Int = 1, qtyHigh: Int = qtyLow) =
        FillDecision(
            anchor = anchor,
            outcome = FillDecision.Outcome.FILL,
            sourceRarity = rarity,
            fill =
                Fill(
                    anchorLine = anchor.line,
                    objSymbol = "obj.snape_grass_seed",
                    rate = RarityFraction.parse(rarity)!!,
                    qtyLow = qtyLow,
                    qtyHigh = qtyHigh,
                ),
        )

    private fun quarantined(anchor: DropAnchor, eliminable: Boolean) =
        FillDecision(
            anchor = anchor,
            outcome = FillDecision.Outcome.QUARANTINE,
            reason = "ambiguous Dropsline rarities and no usable template expectation",
            eliminable = eliminable,
        )

    @Test
    fun `sole leftover anchor takes the sole unmatched candidate`() {
        // The Armoured kraken shape: the page lists the seed twice, the uncseed
        // anchor already matched 1/1,556 x1, and only the 1/810.7 x3 row remains.
        val uncseed = anchor("Snape grass seed", line = 10)
        val rareseed = anchor("Snape grass seed", line = 11)
        val drops = listOf(row("1/810.7", qtyLow = 3), row("1/1,556", qtyLow = 1))
        val decisions = listOf(filled(uncseed, "1/1,556"), quarantined(rareseed, eliminable = true))

        val eliminated = DropRateDecisions.eliminateRemaining(decisions, drops)

        assertEquals(setOf(rareseed), eliminated.keys)
        assertEquals("1/810.7", eliminated[rareseed]?.rarity)
        assertEquals(3, eliminated[rareseed]?.qtyLow)
    }

    @Test
    fun `two leftover anchors cannot be paired`() {
        val a = anchor("Snape grass seed", line = 10)
        val b = anchor("Snape grass seed", line = 11)
        val drops = listOf(row("1/810.7", qtyLow = 3), row("1/1,556", qtyLow = 1))
        val decisions = listOf(quarantined(a, eliminable = true), quarantined(b, eliminable = true))

        assertTrue(DropRateDecisions.eliminateRemaining(decisions, drops).isEmpty())
    }

    @Test
    fun `an unclaimed extra candidate blocks elimination`() {
        val uncseed = anchor("Snape grass seed", line = 10)
        val rareseed = anchor("Snape grass seed", line = 11)
        val drops =
            listOf(row("1/810.7", qtyLow = 3), row("1/1,556", qtyLow = 1), row("1/50", qtyLow = 5))
        val decisions = listOf(filled(uncseed, "1/1,556"), quarantined(rareseed, eliminable = true))

        assertTrue(DropRateDecisions.eliminateRemaining(decisions, drops).isEmpty())
    }

    @Test
    fun `non-eliminable quarantines are never filled`() {
        val a = anchor("Snape grass seed", line = 10)
        val b = anchor("Snape grass seed", line = 11)
        val drops = listOf(row("1/810.7", qtyLow = 3), row("1/1,556", qtyLow = 1))
        val decisions = listOf(filled(a, "1/1,556"), quarantined(b, eliminable = false))

        assertTrue(DropRateDecisions.eliminateRemaining(decisions, drops).isEmpty())
    }

    @Test
    fun `the same probability written two ways is one candidate`() {
        val uncseed = anchor("Snape grass seed", line = 10)
        val rareseed = anchor("Snape grass seed", line = 11)
        // "10/8,107" reduces to the same fraction as "1/810.7".
        val drops =
            listOf(
                row("1/810.7", qtyLow = 3),
                row("10/8,107", qtyLow = 3),
                row("1/1,556", qtyLow = 1),
            )
        val decisions = listOf(filled(uncseed, "1/1,556"), quarantined(rareseed, eliminable = true))

        val eliminated = DropRateDecisions.eliminateRemaining(decisions, drops)

        assertEquals("1/810.7", eliminated[rareseed]?.rarity)
    }

    @Test
    fun `other items in the file do not interfere`() {
        val seed = anchor("Snape grass seed", line = 10)
        val other = anchor("Toadflax seed", line = 11)
        val drops =
            listOf(
                row("1/810.7", qtyLow = 3),
                row("1/1,556", qtyLow = 1),
                row("1/59.5", item = "Toadflax seed"),
            )
        val decisions =
            listOf(
                filled(seed, "1/1,556"),
                quarantined(anchor("Snape grass seed", line = 12), eliminable = true),
                filled(other, "1/59.5"),
            )

        val eliminated = DropRateDecisions.eliminateRemaining(decisions, drops)

        assertEquals(1, eliminated.size)
        assertEquals("1/810.7", eliminated.values.single().rarity)
    }
}
