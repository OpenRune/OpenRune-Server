package org.rsmod.api.type.refs.mod

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.mod.HashedModLevelType
import org.rsmod.game.type.mod.ModLevelType

public abstract class ModLevelReferences : TypeReferences<ModLevelType>() {
    public fun mod(internal: String): ModLevelType {
        val type = HashedModLevelType(null, internal)
        cache += type
        return type
    }
}
