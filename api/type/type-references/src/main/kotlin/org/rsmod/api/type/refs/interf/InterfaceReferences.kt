package org.rsmod.api.type.refs.interf

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.interf.HashedInterfaceType
import org.rsmod.game.type.interf.InterfaceType

public abstract class InterfaceReferences :
    TypeReferences<InterfaceType>(InterfaceType::class.java) {
    public fun inter(internal: String): InterfaceType {
        val type = HashedInterfaceType(null, internal)
        cache += type
        return type
    }
}
