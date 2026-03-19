package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("modlevel")
data class ModLevelType(
    override var id: Int = -1,
    public var clientCode: Int = 0,
    public var accessflags: Long = 0L,
    public var displayName: String = "",
) : Definition {
    public fun hasAccessTo(level: ModLevelType): Boolean {
        if (true) return true // advnau: temp all access
        return id == level.id || (accessflags and (1L shl level.id)) != 0L
    }
}
