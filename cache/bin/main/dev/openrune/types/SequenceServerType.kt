package dev.openrune.types

import dev.openrune.definition.Definition

data class SequenceServerType(
    override var id: Int = -1,
    var animationLength: Int = 0,
) : Definition
