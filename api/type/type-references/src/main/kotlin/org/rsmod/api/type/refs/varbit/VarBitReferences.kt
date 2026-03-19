package org.rsmod.api.type.refs.varbit

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.varbit.HashedVarBitType
import org.rsmod.game.type.varbit.VarBitType

public abstract class VarBitReferences : TypeReferences<VarBitType>() {
    public fun varBit(internal: String): VarBitType {
        val type = HashedVarBitType(null, internal)
        cache += type
        return type
    }
}
