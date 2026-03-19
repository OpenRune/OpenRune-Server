package org.rsmod.api.type.refs.dbtable

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.dbtable.DbTableType
import org.rsmod.game.type.dbtable.HashedDbTableType

public abstract class DbTableReferences : TypeReferences<DbTableType>() {
    public fun dbTable(internal: String): DbTableType {
        val type = HashedDbTableType(null, internal)
        cache += type
        return type
    }
}
