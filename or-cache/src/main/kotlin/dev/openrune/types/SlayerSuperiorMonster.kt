package dev.openrune.types

import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("slayer_superior")
data class SlayerSuperiorMonster(
    var superiorNpc: Int = -1,
    var normalNpcs: List<Int> = emptyList(),
    /** Wiki column "Available in Wilderness" — Krystilia task spawn-rate bonus when true. */
    var wildernessAvailable: Boolean = false,
)
