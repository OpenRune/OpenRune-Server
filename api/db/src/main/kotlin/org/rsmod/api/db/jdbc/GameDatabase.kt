package org.rsmod.api.db.jdbc

import jakarta.inject.Inject
import java.sql.Connection
import java.sql.SQLException
import kotlinx.coroutines.delay
import org.rsmod.api.db.Database
import org.rsmod.api.db.DatabaseConnection
import org.rsmod.api.db.util.DatabaseRollbackException

public class GameDatabase
@Inject
constructor() : Database {
    private lateinit var connection: Connection

    public fun connect(connector: GameConnection) {
        check(!::connection.isInitialized) { "Connection already initialized." }
        val connection = connector.connect()
        try {
            GameSchemaBootstrap.applyIfNeeded(connection)
            GameSchemaBootstrap.ensureOnlineSessionColumns(connection)
            GameSchemaBootstrap.migrateVarpsJsonToCharacterVarpsTable(connection)
            GameSchemaBootstrap.migrateAttrsJsonToCharacterAttrsTable(connection)
            GameSchemaBootstrap.migrateCharacterVarpsVarpIdToVarpKey(connection)
            GameSchemaBootstrap.migrateInventoryStringKeysFromInts(connection)
            GameSchemaBootstrap.migrateInventoryRowIdsToTextComposite(connection)
            GameSchemaBootstrap.ensureRealmDropIgnorePasswords(connection)
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
