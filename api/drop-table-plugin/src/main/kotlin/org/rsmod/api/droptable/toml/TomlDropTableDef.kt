package org.rsmod.api.droptable.toml

public data class TomlDropTableDef(
    val id: String,
    val npcs: List<String> = emptyList(),
    val areas: List<String> = emptyList(),
    val guaranteed: List<TomlGuaranteedEntry> = emptyList(),
    val preRoll: List<TomlChanceEntry> = emptyList(),
    val preRollSeparateRolls: List<TomlSeparateRoll> = emptyList(),
    val main: TomlWeightedSection? = null,
    val tertiary: List<TomlChanceEntry> = emptyList(),
    val brimstoneKeyRoll: Boolean = false,
    val brimstoneKeyRollKonarBonus: Boolean = false,
    val notes: List<String> = emptyList(),
)

public data class TomlWeightedSection(
    val total: Int? = null,
    val name: String? = null,
    val entries: List<TomlWeightedEntry> = emptyList(),
    val separateRolls: List<TomlSeparateRoll> = emptyList(),
)

public data class TomlSeparateRoll(
    val numerator: Int,
    val denominator: Int,
    val entries: List<TomlWeightedEntry> = emptyList(),
)

public data class TomlWeightedEntry(
    val weight: Int,
    val obj: String? = null,
    val shared: String? = null,
    val count: String? = null,
    val countMin: Int? = null,
    val countMax: Int? = null,
    val nothing: Boolean = false,
    val shouldDropLootingBag: Boolean = false,
    val shouldDropBrimstoneKey: Boolean = false,
    val clueScrollBox: Boolean = false,
    val quest: String? = null,
    val questMode: String? = null,
)

public data class TomlGuaranteedEntry(
    val obj: String,
    val count: String? = null,
    val countMin: Int? = null,
    val countMax: Int? = null,
    val shouldDropLootingBag: Boolean = false,
    val shouldDropBrimstoneKey: Boolean = false,
    val clueScrollBox: Boolean = false,
    val quest: String? = null,
    val questMode: String? = null,
)

public data class TomlChanceEntry(
    val numerator: Int,
    val denominator: Int,
    val obj: String,
    val count: String? = null,
    val countMin: Int? = null,
    val countMax: Int? = null,
    val shouldDropLootingBag: Boolean = false,
    val shouldDropBrimstoneKey: Boolean = false,
    val clueScrollBox: Boolean = false,
    val requireRingOfWealth: Boolean = false,
    val excludeRingOfWealth: Boolean = false,
    val requireWilderness: Boolean = false,
    val quest: String? = null,
    val questMode: String? = null,
)

public data class TomlDropHooks(
    val shouldDropLootingBag: Boolean = false,
    val shouldDropBrimstoneKey: Boolean = false,
    val clueScrollBox: Boolean = false,
    val requireRingOfWealth: Boolean = false,
    val excludeRingOfWealth: Boolean = false,
    val requireWilderness: Boolean = false,
    val quest: String? = null,
    val questMode: String? = null,
)
