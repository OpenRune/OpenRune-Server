package org.rsmod.api.type.refs.loc

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.loc.HashedLocType
import org.rsmod.game.type.loc.LocType

public abstract class LocReferences : TypeReferences<LocType>(LocType::class.java) {
    public fun loc(internal: String): LocType {
        val type = HashedLocType(null, internal)
        cache += type
        return type
    }
}
