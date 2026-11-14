package org.rsmod.content.bosses.bryophyta

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.random.SequenceRandom

class BryophytaLootTest {

    @Test
    fun `members chest rolls the members main table`() {
        val random = SequenceRandom().apply {
            next = 0
            then = 1
        }
        val drops = BryophytaChestLoot.roll(members = true, onMossGiantTask = false, random)
        assertEquals(
            listOf(ChestDrop("obj.trail_clue_beginner", 1), ChestDrop("obj.rune_longsword", 1)),
            drops,
        )
    }

    @Test
    fun `free chest rolls the free main table`() {
        val random = SequenceRandom().apply {
            next = 0
            then = 1
        }
        val drops = BryophytaChestLoot.roll(members = false, onMossGiantTask = false, random)
        assertEquals(
            listOf(ChestDrop("obj.trail_clue_beginner", 1), ChestDrop("obj.rune_platelegs", 1)),
            drops,
        )
    }

    @Test
    fun `moss giant task uses the doubled key rate`() {
        val random = SequenceRandom().apply {
            next = 0
            then = 0
        }
        val drops = BryophytaChestLoot.roll(members = true, onMossGiantTask = true, random)
        assertEquals(
            listOf(
                ChestDrop("obj.trail_clue_beginner", 1),
                ChestDrop("obj.rune_longsword", 1),
                ChestDrop("obj.mossy_key", 1),
            ),
            drops,
        )
    }

    @Test
    fun `herb subtable rolls a herb`() {
        val random = SequenceRandom().apply {
            next = 86
            then = 0
            then = 1
        }
        val drops = BryophytaChestLoot.roll(members = true, onMossGiantTask = false, random)
        assertEquals(
            listOf(
                ChestDrop("obj.trail_clue_beginner", 1),
                ChestDrop("obj.unidentified_guam", 1),
            ),
            drops,
        )
    }

    @Test
    fun `chest main tables both total 118`() {
        assertEquals(118, BryophytaChestLoot.mainWeight(members = true))
        assertEquals(118, BryophytaChestLoot.mainWeight(members = false))
    }

    @Test
    fun `boss main tables both total 128`() {
        assertEquals(128, bryophytaMembersMainWeight())
        assertEquals(128, bryophytaFreeMainWeight())
    }
}
