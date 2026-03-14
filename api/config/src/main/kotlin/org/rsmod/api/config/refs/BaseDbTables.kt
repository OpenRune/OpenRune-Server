package org.rsmod.api.config.refs

import org.rsmod.api.type.refs.dbtable.DbTableReferences

typealias dbtables = BaseDbTables

object BaseDbTables : DbTableReferences() {
    val music_modern = dbTable("music_modern")
    val music_classic = dbTable("music_classic")
}
