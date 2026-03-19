package dev.openrune.types.aconverted

import kotlin.contracts.contract

public data class CategoryType(var id: Int) {

    public fun isType(other: CategoryType): Boolean {
        return this.id == other.id
    }

}
