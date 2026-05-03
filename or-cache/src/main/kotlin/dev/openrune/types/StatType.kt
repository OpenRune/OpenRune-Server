package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("stat")
data class StatType(
    override var id: Int = -1,
    public var minLevel: Int = 1,
    public var maxLevel: Int = 99,
    public var displayName: String = "",
    public var unreleased: Boolean = false,
) : Definition {
    fun isType(type: String) = type.asRSCM(RSCMType.STAT) == id
}

data class StatRequirement(val stat: StatType, val level: Int)
