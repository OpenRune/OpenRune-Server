@file:OptIn(UncheckedType::class)

package org.rsmod.game.inv

import dev.openrune.types.ItemServerType
import dev.openrune.types.util.UncheckedType
import kotlin.contracts.contract

public data class InvObj
@UncheckedType("Use the `ItemServerType` constructor instead for type-safety consistency.")
constructor(public val id: Int, public val count: Int, public val vars: Int = 0) {
    public constructor(copy: InvObj) : this(copy.id, copy.count, copy.vars)

    public constructor(
        type: ItemServerType,
        count: Int = 1,
        vars: Int = 0,
    ) : this(type.id, count, vars)
}

public fun InvObj?.isType(type: ItemServerType): Boolean {
    contract { returns(true) implies (this@isType != null) }
    return this != null && type.id == id
}

public fun InvObj?.isAnyType(type1: ItemServerType, type2: ItemServerType): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.id == id || type2.id == id)
}

public fun InvObj?.isAnyType(
    type1: ItemServerType,
    type2: ItemServerType,
    type3: ItemServerType,
): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.id == id || type2.id == id || type3.id == id)
}

public fun InvObj?.isAnyType(
    type1: ItemServerType,
    type2: ItemServerType,
    type3: ItemServerType,
    type4: ItemServerType,
): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.id == id || type2.id == id || type3.id == id || type4.id == id)
}

public fun InvObj?.isAnyType(
    type1: ItemServerType,
    type2: ItemServerType,
    vararg types: ItemServerType,
): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.id == id || type2.id == id || types.any { it.id == id })
}
