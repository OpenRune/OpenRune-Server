package dtx.rs

import dtx.impl.chance.ChanceRollableImpl
import dtx.impl.chance.MultiChanceTable
import dtx.table.Table

internal fun <T, R> mergeInlineSeparateRolls(
    explicit: RSTable<T, R>,
    mainTable: RSTable<T, R>,
): RSTable<T, R> {
    val inline = (mainTable as? RSWeightedTable<T, R>)?.inlineSeparateRolls.orEmpty()
    if (inline.isEmpty()) {
        return explicit
    }

    val explicitEntries = (explicit as? MultiChanceTable<T, R>)?.tableEntries.orEmpty()
    val mergedEntries =
        buildList {
            addAll(explicitEntries)
            addAll(
                inline.map { roll ->
                    ChanceRollableImpl(
                        chance = roll.numerator.toDouble() / roll.denominator.toDouble() * 100.0,
                        rollable = roll.rollable,
                    )
                },
            )
        }

    if (mergedEntries.isEmpty()) {
        return RSPreRollTable.Empty()
    }

    val tableName =
        (explicit as? Table<T, R>)?.tableIdentifier?.takeUnless { it.isBlank() }
            ?: "Separate Rolls"

    return RSPreRollTable(tableName, mergedEntries)
}
