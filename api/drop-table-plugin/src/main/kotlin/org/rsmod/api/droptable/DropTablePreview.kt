package org.rsmod.api.droptable

import dtx.core.AllOf
import dtx.core.AnyOf
import dtx.core.Rollable
import dtx.core.Single
import dtx.impl.chance.ChanceRollable
import dtx.impl.weighted.WeightedCollectionRollable
import dtx.impl.weighted.WeightedRollable
import dtx.impl.weighted.WeightedTable
import dtx.rs.RSDropTable
import dtx.table.Table
import org.rsmod.game.entity.Player

public data class DropPreviewEntry(
    public val item: DropRollItem?,
    public val stage: String,
    public val baseChance: Double,
    public val rolls: Int = 1,
)

/** Inspects definitions only: no RNG, condition, transform or reward hook is executed. */
public object DropTablePreview {
    public fun entries(table: RSDropTable<Player, DropRollItem>): List<DropPreviewEntry> = buildList {
        val stages = listOf("Always", "Pre-roll", "Separate", "Main", "Tertiary")
        table.tableEntries.forEachIndexed { index, rollable ->
            visit(rollable, stages[index], 1.0, if (index == 3) table.mainRolls else 1, 0, this)
        }
    }

    private fun visit(
        node: Rollable<Player, DropRollItem>,
        stage: String,
        chance: Double,
        rolls: Int,
        depth: Int,
        result: MutableList<DropPreviewEntry>,
    ) {
        if (depth > 32 || result.size >= 2_000) return
        fun child(next: Rollable<Player, DropRollItem>, factor: Double = 1.0) =
            visit(next, stage, chance * factor, rolls, depth + 1, result)
        when (node) {
            is Single -> addItem(node.result, stage, chance, rolls, result)
            is PreviewableDrop -> addItem(node.item, stage, chance, rolls, result)
            is WeightedCollectionRollable -> {
                val choices = node.rollables.filter { it.weight > 0 }
                choices.forEach { child(it, 1.0 / choices.size) }
            }
            is WeightedTable -> {
                val total = node.tableEntries.sumOf { it.weight }
                if (total > 0) node.tableEntries.forEach { child(it, it.weight / total) }
            }
            is ChanceRollable -> child(node.rollable, node.chance / 100.0)
            is WeightedRollable -> child(node.rollable)
            is Table -> node.tableEntries.forEach { child(it) }
            is AllOf -> node.rollables.forEach { child(it) }
            is AnyOf -> node.rollables.forEach { child(it, 1.0 / node.rollables.size) }
            else -> result += DropPreviewEntry(null, stage, chance, rolls)
        }
    }

    private fun addItem(
        item: DropRollItem, stage: String, chance: Double, rolls: Int,
        result: MutableList<DropPreviewEntry>,
    ) {
        if (item.isNothing) return
        result += DropPreviewEntry(item, stage, chance, rolls)
        item.bonusDrops.forEach { result += DropPreviewEntry(it, "$stage bonus", chance, rolls) }
    }
}
