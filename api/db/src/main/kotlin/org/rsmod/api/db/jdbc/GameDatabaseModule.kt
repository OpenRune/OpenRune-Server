package org.rsmod.api.db.jdbc

import com.google.inject.Provider
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.db.Database
import org.rsmod.api.db.DatabaseConfig
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.module.ExtendedModule
import org.rsmod.server.services.Service

public object GameDatabaseModule : ExtendedModule() {
    override fun bind() {
        bind(DatabaseConfig::class.java).toProvider(DatabaseConfigProvider::class.java)
        bindInstance<GameConnection>()
        bindBaseAndImpl<Database>(GameDatabase::class.java)
        addSetBinding<Service>(GameDatabaseService::class.java)
    }

    @Singleton
    private class DatabaseConfigProvider
    @Inject
    constructor(
        private val serverConfig: ServerConfig,
    ) : Provider<DatabaseConfig> {
        override fun get(): DatabaseConfig = DatabaseConfig.create(serverConfig)
    }
}
