package org.rsmod.api.type.refs.stat

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.stat.HashedStatType
import org.rsmod.game.type.stat.StatType

public abstract class StatReferences : TypeReferences<StatType>(StatType::class.java) {
    public fun stat(internal: String): StatType {
        val type = HashedStatType(null, internal)
        cache += type
        return type
    }
}
