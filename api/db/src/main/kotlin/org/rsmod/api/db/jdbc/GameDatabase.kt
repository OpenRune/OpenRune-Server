package org.rsmod.api.db.jdbc

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Provider
import java.sql.Connection
import java.sql.SQLException
import kotlinx.coroutines.delay
import org.rsmod.api.db.Database
import org.rsmod.api.db.DatabaseConfig
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.db.util.DatabaseRollbackException

public class GameDatabase
@Inject
constructor(
    private val configProvider: Provider<DatabaseConfig>,
) : Database {
    private val logger = InlineLogger()
    private lateinit var connection: Connection

    public fun connect(connector: GameConnection) {
        check(!::connection.isInitialized) { "Connection already initialized." }
        val connection = connector.connect()
        val embeddedRecovery = configProvider.get().usesEmbeddedPostgres
        try {
            runBootstrapWithEmbeddedNukeOnFailure(connection, embeddedRecovery)
            connection.commit()
        } catch (t: Throwable) {
            try {
                connection.rollback()
            } catch (_: Throwable) {
                // ignore
            }
            throw t
        }
        this.connection = connection
    }

    private fun runBootstrapWithEmbeddedNukeOnFailure(
        connection: Connection,
        embeddedRecovery: Boolean,
    ) {
        var attempt = 0
        while (true) {
            try {
                GameSchemaBootstrap.applyIfNeeded(connection)
                GameSchemaBootstrap.ensureOnlineSessionColumns(connection)
                GameSchemaBootstrap.ensureRealmDropIgnorePasswords(connection)
                return
            } catch (t: Throwable) {
                try {
                    connection.rollback()
                } catch (_: Throwable) {
                    // ignore
                }
                if (!embeddedRecovery || attempt >= 1) {
                    throw t
                }
                logger.warn(t) {
                    "Embedded PostgreSQL: game DB bootstrap failed; dropping all objects in schema " +
                        "`public` and retrying once."
                }
                GameSchemaBootstrap.resetPublicSchema(connection)
                connection.commit()
                attempt++
            }
        }
    }

    public fun close() {
        assertValidConnection()
        this.connection.close()
    }

    override suspend fun <T> withTransaction(block: (DatabaseConnection) -> T): T =
        withConnection { connection ->
            val wrapped = DatabaseConnection(connection)
            try {
                val result = block(wrapped)
                connection.commit()
                result
            } catch (t: Throwable) {
                try {
                    connection.rollback()
                } catch (rollbackEx: Throwable) {
                    throw DatabaseRollbackException(t, rollbackEx)
                }
                throw t
            }
        }

    private suspend fun <T> withConnection(block: (Connection) -> T): T {
        assertValidConnection()
        repeat(MAX_ATTEMPTS - 1) {
            try {
                return block(connection)
            } catch (_: SQLException) {
                delay(BACKOFF_MILLIS)
            }
        }
        return block(connection)
    }

    private fun assertValidConnection() {
        check(::connection.isInitialized) { "Connection was not initialized." }
        check(!connection.isClosed) { "Connection is closed." }
    }

    private companion object {
        private const val MAX_ATTEMPTS = 3
        private const val BACKOFF_MILLIS = 10L
    }
}
