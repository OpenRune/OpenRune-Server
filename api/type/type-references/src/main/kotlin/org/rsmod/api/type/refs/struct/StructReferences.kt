package org.rsmod.api.type.refs.struct

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.struct.HashedStructType
import org.rsmod.game.type.struct.StructType

public abstract class StructReferences : TypeReferences<StructType>(StructType::class.java) {
    public fun struct(internal: String): StructType {
        val type = HashedStructType(null, internal)
        cache += type
        return type
    }
}
