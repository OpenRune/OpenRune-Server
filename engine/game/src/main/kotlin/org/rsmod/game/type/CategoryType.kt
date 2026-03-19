package org.rsmod.game.type

import dev.openrune.types.aconverted.CategoryType
import kotlin.contracts.contract
import org.rsmod.game.inv.InvObj

public fun CategoryType?.isAssociatedWith(obj: InvObj?): Boolean {
    contract { returns(true) implies (this@isAssociatedWith != null && obj != null) }
    return this != null && obj != null && obj.id == id
}
