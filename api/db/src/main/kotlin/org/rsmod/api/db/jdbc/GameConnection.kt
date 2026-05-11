package org.rsmod.api.db.jdbc

import jakarta.inject.Inject
import jakarta.inject.Provider
import java.sql.Connection
import java.sql.DriverManager
import org.rsmod.api.db.DatabaseConfig

public class GameConnection
@Inject
constructor(
    private val configProvider: Provider<DatabaseConfig>,
) {
    public fun connect(): Connection {
        val config = configProvider.get()
        val connection =
            DriverManager.getConnection(
                config.jdbcUrl,
                config.user,
                config.password,
            )
        connection.autoCommit = false
        return connection
    }
}
