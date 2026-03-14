package org.rsmod.api.type.refs.inv

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.inv.HashedInvType
import org.rsmod.game.type.inv.InvType

public abstract class InvReferences : TypeReferences<InvType>(InvType::class.java) {
    public fun inv(internal: String): InvType {
        val type = HashedInvType(null, internal)
        cache += type
        return type
    }
}
