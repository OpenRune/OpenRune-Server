package org.rsmod.api.type.refs.npc

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.npc.HashedNpcType
import org.rsmod.game.type.npc.NpcType

public abstract class NpcReferences : TypeReferences<NpcType>(NpcType::class.java) {
    public fun npc(internal: String): NpcType {
        val type = HashedNpcType(null, internal)
        cache += type
        return type
    }
}
