package org.rsmod.api.type.refs.hunt

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.hunt.HashedHuntModeType
import org.rsmod.game.type.hunt.HuntModeType

public abstract class HuntModeReferences : TypeReferences<HuntModeType>(HuntModeType::class.java) {
    public fun hunt(internal: String): HuntModeType {
        val type = HashedHuntModeType(null, internal)
        cache += type
        return type
    }
}
