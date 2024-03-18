package gg.rsmod.cache.definition.data

import gg.rsmod.cache.definition.Definition

data class VarBitDefinition(
    override var id: Int = -1,
    var varp: Int = 0,
    var startBit: Int = 0,
    var endBit: Int = 0
) : Definition