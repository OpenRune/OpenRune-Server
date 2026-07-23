package org.rsmod.content.generic.killcount.toml

public data class TomlKillcountDef(val killcounts: List<TomlKillcountEntry> = emptyList())

public data class TomlKillcountEntry(
    val npcs: List<String>,
    val varbit: String,
    val notify: Boolean = true,
)
