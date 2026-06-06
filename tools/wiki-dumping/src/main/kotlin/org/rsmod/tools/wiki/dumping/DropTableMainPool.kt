package org.rsmod.tools.wiki.dumping

private val TOML_SHARED_TABLE_NAMES =
    setOf("herb", "usefulHerb", "combatHerb", "gem", "seed", "rareSeed", "megaRare", "rareDrop")

internal fun computePoolPaddingWeight(
    main: List<ResolvedDropEntry>,
    mainMaxRoll: Int?,
    subtableAccesses: List<ResolvedSubtableAccess>,
): Int {
    val maxRoll = mainMaxRoll ?: return 0
    val used = main.sumOf { it.weight ?: 0 } + subtableAccesses.sumOf { it.numerator }
    return (maxRoll - used).coerceAtLeast(0)
}

internal fun GeneratedDropTableSpec.poolPaddingWeight(): Int =
    computePoolPaddingWeight(main, mainMaxRoll, subtableAccesses)

internal fun ResolvedSubtableAccess.isTomlExportableSharedAccess(): Boolean {
    if (needsHardcodedSharedTable || !herbRollVariants.isNullOrEmpty()) {
        return false
    }
    val ref = tableRef.removePrefix("SharedDropTables.")
    return ref in TOML_SHARED_TABLE_NAMES
}

internal const val POOL_PADDING_TOML_NOTE =
    "Main table pool padding uses a nothing roll (F2P drops removed and/or subtable access missing from wiki parse)"
