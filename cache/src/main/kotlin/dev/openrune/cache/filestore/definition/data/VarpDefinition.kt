package dev.openrune.cache.filestore.definition.data

import dev.openrune.cache.filestore.definition.Definition

data class VarpDefinition(
    override var id: Int = -1,
    var configType: Int = 0,
) : Definition