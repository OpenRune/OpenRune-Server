package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("varn")
data class VarnType(override var id: Int = -1, public var bitProtect: Boolean = true) : Definition
