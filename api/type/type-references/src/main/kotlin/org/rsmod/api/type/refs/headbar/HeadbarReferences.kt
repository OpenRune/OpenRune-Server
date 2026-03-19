package org.rsmod.api.type.refs.headbar

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.headbar.HashedHeadbarType
import org.rsmod.game.type.headbar.HeadbarType

public abstract class HeadbarReferences : TypeReferences<HeadbarType>() {
    public fun headbar(internal: String): HeadbarType {
        val type = HashedHeadbarType(null, internal)
        cache += type
        return type
    }
}
