package dev.openrune

import dev.openrune.definition.Definition
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.ItemServerType

val Definition.internalName: String
    get() = when(this) {
        is ItemServerType -> RSCM.getReverseMapping(RSCMType.OBJ, id)
        else -> error("Unsupported definition type: $this")
    }
