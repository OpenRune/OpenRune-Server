package dev.openrune.types

import dev.openrune.ServerCacheManager
import dev.openrune.definition.Definition
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("modlevel")
data class ModLevelType(
    override var id: Int = -1,
    public var clientCode: Int = 0,
    public var accessflags: Long = 0L,
    public var displayName: String = "",
) : Definition {
    public fun hasAccessTo(internal: String): Boolean {
        val level = ServerCacheManager.getModLevel(internal.asRSCM(RSCMType.MODLEVEL))?:
            error("Error finding mod level: $internal")

        return id == level.id || (accessflags and (1L shl level.id)) != 0L
    }
}
