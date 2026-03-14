package org.rsmod.api.type.refs.comp

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.comp.ComponentType
import org.rsmod.game.type.comp.HashedComponentType

public abstract class ComponentReferences :
    TypeReferences<ComponentType>(ComponentType::class.java) {
    public fun component(internal: String): ComponentType {
        val type = HashedComponentType(null, internal)
        cache += type
        return type
    }
}
