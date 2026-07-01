package org.rsmod.api.db.jdbc

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.rsmod.api.db.DatabaseConfig
import org.rsmod.server.services.Service

public class GameDatabaseService
@Inject
constructor(
    private val configProvider: Provider<DatabaseConfig>,
    private val connector: GameConnection,
    private val database: GameDatabase,
) : Service {
    private val logger = InlineLogger()

    private var databaseConnected = false

    override suspend fun startup() {
        connectDataSource()
    }

    private fun connectDataSource() {
        database.connect(connector)
        databaseConnected = true
    }

    override suspend fun shutdown() {
        logger.info { "Attempting to shut down game database service." }
        try {
            if (databaseConnected) {
                database.close()
            }
            logger.info { "Game database service successfully shut down." }
        } catch (t: Throwable) {
            logger.error(t) { "Game database service failed to shut down." }
        }
    }
}
