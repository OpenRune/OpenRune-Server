package gg.rsmod.cache.definition.data

import gg.rsmod.cache.definition.Definition

data class VarpDefinition(
    override var id: Int = -1,
    var configType: Int = 0,
) : Definition