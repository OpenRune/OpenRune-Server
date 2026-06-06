package org.rsmod.tools.wiki.dumping.wiki

enum class SubtableKey(val codegenRef: String, val wikiLabel: String) {
    HERB("SharedDropTables.herb", "herb drop table"),
    HERB_MULTI("", "multi-roll herb drop table"),
    USEFUL_HERB("SharedDropTables.usefulHerb", "useful herb drop table"),
    COMBAT_HERB("SharedDropTables.combatHerb", "combat herb drop table"),
    GEM("SharedDropTables.gem", "gem drop table"),
    SEED("SharedDropTables.seed", "general seed drop table"),
    RARE_SEED("SharedDropTables.rareSeed", "rare seed drop table"),
    RDT("SharedDropTables.rareDrop", "rare drop table"),
    MEGA_RARE("SharedDropTables.megaRare", "mega-rare drop table"),
    ;

    val needsHardcodedSharedTable: Boolean get() = this == MEGA_RARE
}

data class ParsedSubtableAccess(
    val tableKey: SubtableKey,
    val numerator: Int,
    val denominator: Int,
    val subsection: String,
    val fromProse: Boolean = false,
    val herbRollVariants: List<HerbRollVariant>? = null,
)
