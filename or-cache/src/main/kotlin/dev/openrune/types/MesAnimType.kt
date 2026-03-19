package dev.openrune.types

import dev.openrune.definition.Definition
import dev.openrune.toml.rsconfig.RsTableHeaders

@RsTableHeaders("mesanim")
data class MesAnimType(
    override var id: Int = -1,
    public var len1: Int = -1,
    public var len2: Int = -1,
    public var len3: Int = -1,
    public var len4: Int = -1,
) : Definition
