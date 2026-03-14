package org.rsmod.api.type.refs.model

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.model.HashedModelType
import org.rsmod.game.type.model.ModelType

public abstract class ModelReferences : TypeReferences<ModelType>(ModelType::class.java) {
    public fun model(internal: String): ModelType {
        val type = HashedModelType(null, internal)
        cache += type
        return type
    }
}
