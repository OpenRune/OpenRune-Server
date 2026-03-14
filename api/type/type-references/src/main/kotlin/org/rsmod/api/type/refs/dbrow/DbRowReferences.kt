package org.rsmod.api.type.refs.dbrow

import org.rsmod.api.type.refs.TypeReferences
import org.rsmod.game.type.dbrow.DbRowType
import org.rsmod.game.type.dbrow.HashedDbRowType

public abstract class DbRowReferences : TypeReferences<DbRowType>(DbRowType::class.java) {
    public fun dbRow(internal: String): DbRowType {
        val type = HashedDbRowType(null, internal)
        cache += type
        return type
    }
}
