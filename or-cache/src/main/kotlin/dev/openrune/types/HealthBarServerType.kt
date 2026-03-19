package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("health")
data class HealthBarServerType(override var id: Int = -1, var segments: Int = 30) : Definition
