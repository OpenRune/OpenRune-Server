package org.rsmod.api.type.refs.content

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.content.ContentGroupType
import org.rsmod.game.type.content.ContentGroupTypeBuilder

public abstract class ContentReferences :
    TypeReferences<ContentGroupType>(ContentGroupType::class.java) {
    public fun content(internal: String): ContentGroupType {
        val type = ContentGroupTypeBuilder(internal).build(id = -1)
        cache += type
        return type
    }
}
