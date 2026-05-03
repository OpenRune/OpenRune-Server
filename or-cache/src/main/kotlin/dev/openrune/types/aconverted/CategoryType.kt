package dev.openrune.types.aconverted

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import kotlin.contracts.contract

public data class CategoryType(var id: Int) {

    public fun isType(other: String): Boolean {
        return this.id == other.asRSCM(RSCMType.CATEGORY)
    }

}
