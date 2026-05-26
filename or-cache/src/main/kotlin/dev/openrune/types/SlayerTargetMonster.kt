package dev.openrune.types

import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("slayer_target_monster")
data class SlayerTargetMonster(
    var targetId: Int = -1,
    var targets: List<Int> = emptyList(),
)
