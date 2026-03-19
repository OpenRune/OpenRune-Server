package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("varobj")
data class VarObjBitType(override var id: Int = -1, var startBit: Int = -1, var endBit: Int = -1) :
    Definition {

    val bits: IntRange
        get() = startBit..endBit
}
