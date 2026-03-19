package org.rsmod.api.type.refs.obj

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.obj.HashedObjType
import org.rsmod.game.type.obj.ObjType

public abstract class ObjReferences : TypeReferences<ObjType>() {
    public fun obj(internal: String): ObjType {
        val type = HashedObjType(null, internal)
        cache += type
        return type
    }
}
