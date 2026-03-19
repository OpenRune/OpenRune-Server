package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("stat")
data class StatType(
    override var id: Int = -1,
    public var minLevel: Int = 1,
    public var maxLevel: Int = 99,
    public var displayName: String = "",
    public var unreleased: Boolean = false,
) : Definition {
    fun isType(type: StatType) = type.id == id
}

data class StatRequirement(val stat: StatType, val level: Int)
