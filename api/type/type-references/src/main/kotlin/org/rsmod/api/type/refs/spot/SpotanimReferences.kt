package org.rsmod.api.type.refs.spot

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.spot.HashedSpotanimType
import org.rsmod.game.type.spot.SpotanimType

public abstract class SpotanimReferences : TypeReferences<SpotanimType>() {
    public fun spotAnim(internal: String): SpotanimType {
        val type = HashedSpotanimType(null, internal)
        cache += type
        return type
    }
}
