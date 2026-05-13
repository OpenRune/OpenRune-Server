package org.rsmod.api.db

import org.rsmod.api.db.jdbc.GameDatabaseModule
import org.rsmod.module.ExtendedModule

public object DatabaseModule : ExtendedModule() {
    override fun bind() {
        install(GameDatabaseModule)
    }
}
