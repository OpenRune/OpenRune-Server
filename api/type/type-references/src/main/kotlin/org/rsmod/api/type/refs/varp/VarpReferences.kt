package org.rsmod.api.type.refs.varp

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.varp.HashedVarpType
import org.rsmod.game.type.varp.VarpType

public abstract class VarpReferences : TypeReferences<VarpType>() {
    public fun varp(internal: String): VarpType {
        val type = HashedVarpType(null, internal)
        cache += type
        return type
    }
}
