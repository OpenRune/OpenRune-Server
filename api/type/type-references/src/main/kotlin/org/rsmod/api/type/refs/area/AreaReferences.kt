package org.rsmod.api.type.refs.area

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.area.AreaType
import org.rsmod.game.type.area.HashedAreaType

public abstract class AreaReferences : TypeReferences<AreaType>(AreaType::class.java) {
    public fun area(internal: String): AreaType {
        val type = HashedAreaType(null, internal)
        cache += type
        return type
    }
}
