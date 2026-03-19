package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("projectile")
data class ProjAnimType(
    override var id: Int = -1,
    var startHeight: Int = 0,
    var endHeight: Int = 0,
    var delay: Int = 0,
    var angle: Int = 0,
    var lengthAdjustment: Int = 0,
    var progress: Int = 11,
    var stepMultiplier: Int = 5,
) : Definition
