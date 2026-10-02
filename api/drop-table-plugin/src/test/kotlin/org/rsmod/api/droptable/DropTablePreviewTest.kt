package org.rsmod.api.droptable

import dtx.core.singleRollable
import dtx.rs.RSDropTable
import dtx.rs.rsGuaranteedTable
import dtx.rs.rsWeightedTable
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.rsmod.game.entity.Player

class DropTablePreviewTest {
    @Test
    fun `nested table probabilities quantities and main roll count are preserved`() {
        val bones = DropRollItem("obj.bones", 1)
        val runes = DropRollItem("obj.chaosrune", 10..30)
        val nested = rsWeightedTable<Player, DropRollItem>(4) {
            1 weight runes
            3 weight nothingDrop()
        }
        val table = RSDropTable(
            tableIdentifier = "test", mainRolls = 2,
            guaranteed = rsGuaranteedTable<Player, DropRollItem> { add(bones) },
            mainTable = rsWeightedTable<Player, DropRollItem>(8) {
                2 weight nested
                6 weight nothingDrop()
            },
        )
        val rows = DropTablePreview.entries(table)
        assertEquals(2, rows.size)
        assertEquals(1.0, rows[0].baseChance)
        assertEquals(1.0 / 16, rows[1].baseChance)
        assertEquals(2, rows[1].rolls)
        assertEquals(10..30, rows[1].item?.count)
    }

    @Test
    fun `preview never invokes conditions transforms dynamic results or completion hooks`() {
        var invoked = 0
        val item = DropRollItem("obj.coins", 10,
            condition = { invoked++; true }, transformObj = { invoked++; null },
            killCondition = { _, _, _ -> invoked++; true })
        val dynamic = singleRollable<Player, DropRollItem> {
            selectResult { _, _ -> invoked++; error("Preview must not roll") }
            onRollCompleted { _, _, _ -> invoked++ }
        }
        val table = RSDropTable("test", mainTable = rsWeightedTable<Player, DropRollItem>(2) {
            1 weight dropRollable(item)
            1 weight dynamic
        })
        val rows = DropTablePreview.entries(table)
        assertEquals(0, invoked)
        assertEquals(item, rows[0].item)
        assertNull(rows[1].item)
    }

    @Test
    fun `chance wrappers and bonus definitions remain visible without giving rewards`() {
        val bonus = DropRollItem("obj.bones", 1)
        val item = DropRollItem("obj.coins", 50, bonusDrops = listOf(bonus))
        val table = RSDropTable<Player, DropRollItem>("test",
            tertiaries = dtx.rs.rsPrerollTable {
                (1 outOf 100) rolls dropRollable(item)
            })
        val rows = DropTablePreview.entries(table)
        assertEquals(listOf("obj.coins", "obj.bones"), rows.map { it.item?.obj })
        assertTrue(rows.all { it.baseChance == 0.01 })
    }
}
