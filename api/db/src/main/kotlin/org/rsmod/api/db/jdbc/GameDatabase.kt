package org.rsmod.api.db.jdbc

import com.github.michaelbull.logging.InlineLogger
import dev.or2.central.db.FlywayMigrator
import jakarta.inject.Inject
import jakarta.inject.Provider
import java.sql.Connection
import java.sql.SQLException
import kotlinx.coroutines.delay
import org.rsmod.api.db.Database
import org.rsmod.api.db.DatabaseConfig
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.db.util.DatabaseRollbackException
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.world.WorldType

public class GameDatabase
@Inject
constructor(
    private val configProvider: Provider<DatabaseConfig>,
    private val serverConfigProvider: Provider<ServerConfig>,
) : Database {
    private val logger = InlineLogger()

    private lateinit var connection: Connection

    public fun connect(connector: GameConnection) {
        check(!::connection.isInitialized) { "Connection already initialized." }
        val config = configProvider.get()
        val connection = connector.connect()
        try {
            FlywayMigrator.migrate(config.jdbcUrl, config.user, config.password)
            migrateWorldTypeSchemas(config)
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

    public fun close() {
        assertValidConnection()
        this.connection.close()
    }

    /** Central owns `public` and `main`; only this world knows which other modes it serves. */
    private fun migrateWorldTypeSchemas(config: DatabaseConfig) {
        val served = WorldType.supportedFrom(serverConfigProvider.get().worldTypes)
        FlywayMigrator.migrateWorldTypes(
            config.jdbcUrl,
            config.user,
            config.password,
            served.map { it.key },
        )
    }

    override suspend fun <T> withTransaction(block: (DatabaseConnection) -> T): T =
        withSchemaTransaction(null, block)

    override suspend fun <T> withSchemaTransaction(
        schema: String?,
        block: (DatabaseConnection) -> T,
    ): T =
        withConnection { connection ->
            val wrapped = DatabaseConnection(connection)
            try {
                applySchemaSearchPath(connection, schema)
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

    /** Blocking variant for the game thread (avoids [runBlocking] coroutine overhead). */
    public fun <T> withTransactionBlocking(
        schema: String? = null,
        block: (DatabaseConnection) -> T,
    ): T {
        assertValidConnection()
        try {
            val wrapped = DatabaseConnection(connection)
            applySchemaSearchPath(connection, schema)
            val result = block(wrapped)
            connection.commit()
            return result
        } catch (t: Throwable) {
            try {
                connection.rollback()
            } catch (rollbackEx: Throwable) {
                throw DatabaseRollbackException(t, rollbackEx)
            }
            throw t
        }
    }

    /** `SET LOCAL` reverts on commit, so a scoped transaction cannot leak into the next one. */
    private fun applySchemaSearchPath(connection: Connection, schema: String?) {
        if (schema == null) {
            return
        }
        require(SCHEMA_NAME_PATTERN.matches(schema)) {
            "Not a valid schema identifier: '$schema'"
        }
        connection.createStatement().use { statement ->
            statement.execute("SET LOCAL search_path TO \"$schema\", public")
        }
        verifySchemaSearchPath(connection, schema)
    }

    /**
     * Postgres ignores a missing schema in `search_path`, and `SET LOCAL` is a no-op outside a
     * transaction; either way queries silently fall back to `public` and hit the wrong mode's save.
     * Thrown rather than logged, because that corrupts one save with another.
     */
    private fun verifySchemaSearchPath(connection: Connection, schema: String) {
        val effective =
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT current_schema()").use { rs ->
                    if (rs.next()) rs.getString(1) else null
                }
            }
        if (effective == schema) {
            return
        }
        val reason =
            if (connection.autoCommit) {
                "connection is in autocommit mode, so `SET LOCAL` did not stick"
            } else {
                "schema '$schema' does not exist - it may have been dropped while the server was " +
                    "running; a restart recreates and migrates it"
            }
        logger.error { "Schema scoping failed: requested='$schema' effective='$effective' ($reason)" }
        error("Could not scope transaction to schema '$schema': $reason")
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

        /** Interpolated as an identifier, so it must be constrained. */
        private val SCHEMA_NAME_PATTERN = Regex("^[a-z][a-z0-9_]{0,62}$")
    }
}
