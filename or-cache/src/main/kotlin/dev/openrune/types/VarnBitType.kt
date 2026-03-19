package dev.openrune.types

import dev.openrune.ServerCacheManager
import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("varnbit")
data class VarnBitType(
    override var id: Int = -1,
    var varn: Int = -1,
    var lsb: Int = -1,
    var msb: Int = -1,
) : Definition {

    val baseVar: VarnType
        get() = ServerCacheManager.getVarn(varn) ?: error("Unable to find varn: $varn")

    val bits: IntRange
        get() = lsb..msb
}
