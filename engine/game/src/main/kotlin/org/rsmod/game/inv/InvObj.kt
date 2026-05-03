@file:OptIn(UncheckedType::class)

package org.rsmod.game.inv

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
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

    public constructor(
        type: String,
        count: Int = 1,
        vars: Int = 0,
    ) : this(type.asRSCM(RSCMType.OBJ), count, vars)
}

public fun InvObj?.isType(type: ItemServerType): Boolean {
    contract { returns(true) implies (this@isType != null) }
    return this != null && type.id == id
}

public fun InvObj?.isType(type: String): Boolean {
    contract { returns(true) implies (this@isType != null) }
    return this != null && type.asRSCM(RSCMType.OBJ) == id
}

public fun InvObj?.isAnyType(type1: ItemServerType, type2: ItemServerType): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.id == id || type2.id == id)
}

public fun InvObj?.isAnyType(
    type1: String,
    type2: String,
    type3: String,
): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.asRSCM(RSCMType.OBJ) == id || type2.asRSCM(RSCMType.OBJ) == id || type3.asRSCM(RSCMType.OBJ) == id)
}

public fun InvObj?.isAnyType(
    type1: String,
    type2: String,
    type3: String,
    type4: String,
): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.asRSCM(RSCMType.OBJ) == id || type2.asRSCM(RSCMType.OBJ) == id || type3.asRSCM(RSCMType.OBJ) == id || type4.asRSCM(RSCMType.OBJ) == id)
}

public fun InvObj?.isAnyType(
    type1: String,
    type2: String,
    vararg types: String,
): Boolean {
    contract { returns(true) implies (this@isAnyType != null) }
    return this != null && (type1.asRSCM(RSCMType.OBJ) == id || type2.asRSCM(RSCMType.OBJ) == id || types.any { it.asRSCM(RSCMType.OBJ) == id })
}
