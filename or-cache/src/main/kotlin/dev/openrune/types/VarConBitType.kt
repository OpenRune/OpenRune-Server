package dev.openrune.types

import dev.openrune.ServerCacheManager
import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("varconbit")
data class VarConBitType(
    override var id: Int = -1,
    var varcon: Int = -1,
    var lsb: Int = -1,
    var msb: Int = -1,
) : Definition {

    val baseVar: VarConType
        get() = ServerCacheManager.getVarCon(varcon) ?: error("Unable to find varn: $varcon")

    val bits: IntRange
        get() = lsb..msb
}
