package dev.openrune.types

import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("npc_tip")
data class SlayerNpcTip(
    var targets: List<Int> = emptyList(),
    var tip: String = "",
)
