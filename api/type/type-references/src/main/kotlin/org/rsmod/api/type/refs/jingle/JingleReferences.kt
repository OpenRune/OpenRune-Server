package org.rsmod.api.type.refs.jingle

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.jingle.JingleType
import org.rsmod.game.type.jingle.JingleTypeBuilder

public abstract class JingleReferences : TypeReferences<JingleType>() {
    public fun jingle(internal: String): JingleType {
        val type = JingleTypeBuilder(internal).build(id = -1)
        cache += type
        return type
    }
}
