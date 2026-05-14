package org.rsmod.api.db.jdbc

import com.github.michaelbull.logging.InlineLogger
import dev.or2.sql.OpenRuneSql
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.sql.Connection

internal object GameSchemaBootstrap {
    private val logger = InlineLogger()

    /** Drops everything in `public` (embedded dev recovery). Caller must commit. */
    internal fun resetPublicSchema(connection: Connection) {
        PostgresPublicSchemaReset.dropAllInPublicSchema(connection)
    }

    fun applyIfNeeded(connection: Connection) {
        if (coreGameSchemaPresent(connection)) {
            return
        }
        val resource = "db/game-schema-postgres.sql"
        val stream =
            Thread.currentThread().contextClassLoader.getResourceAsStream(resource)
                ?: error("Missing classpath resource $resource")
        val text =
            stream.use { ins ->
                BufferedReader(InputStreamReader(ins, StandardCharsets.UTF_8)).readText()
            }
        val statements =
            text
                .split(';')
                .asSequence()
                .map { chunk ->
                    chunk
                        .lines()
                        .filterNot { it.trim().startsWith("--") }
                        .joinToString("\n")
                        .trim()
                }
                .filter { it.isNotEmpty() }
                .toList()
        connection.createStatement().use { statement ->
            for (sql in statements) {
                statement.execute(sql)
            }
        }
    }

    fun ensureOnlineSessionColumns(connection: Connection) {
        if (!tableExists(connection, "account_characters")) {
            return
        }
        val columns = columnNames(connection, "account_characters")
        if (!columns.contains("online_central_world_id")) {
            connection
                .createStatement()
                .execute("ALTER TABLE account_characters ADD COLUMN online_central_world_id INTEGER NULL")
        }
        if (!columns.contains("online_session_heartbeat")) {
            connection
                .createStatement()
                .execute("ALTER TABLE account_characters ADD COLUMN online_session_heartbeat TIMESTAMP NULL")
        }
    }

    fun ensureRealmDropIgnorePasswords(connection: Connection) {
        if (!tableExists(connection, "realms")) {
            return
        }
        val columns = columnNames(connection, "realms")
        if (!columns.contains("ignore_passwords")) {
            return
        }
        try {
            connection.createStatement().execute("ALTER TABLE realms DROP COLUMN ignore_passwords")
        } catch (_: Exception) {
            // ignore
        }
    }

    private fun columnDataType(
        connection: Connection,
        table: String,
        column: String,
    ): String? {
        val sql =
            """
            SELECT data_type FROM information_schema.columns
            WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
            """.trimIndent()
        return connection.prepareStatement(sql).use { ps ->
            ps.setString(1, table.lowercase())
            ps.setString(2, column.lowercase())
            ps.executeQuery().use { rs ->
                if (rs.next()) rs.getString(1) else null
            }
        }
    }

    private fun columnNames(
        connection: Connection,
        table: String,
    ): Set<String> {
        val names = mutableSetOf<String>()
        val sql = OpenRuneSql.text("game/schema/column_names.sql")
        connection.prepareStatement(sql).use { ps ->
            ps.setString(1, table.lowercase())
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    names += rs.getString("column_name").lowercase()
                }
            }
        }
        return names
    }

    private fun tableExists(
        connection: Connection,
        name: String,
    ): Boolean {
        val sql = OpenRuneSql.text("game/schema/table_exists.sql")
        return connection.prepareStatement(sql).use { ps ->
            ps.setString(1, "public.$name")
            ps.executeQuery().use { rs ->
                rs.next() && rs.getBoolean(1)
            }
        }
    }

    private fun coreGameSchemaPresent(connection: Connection): Boolean =
        tableExists(connection, "accounts") &&
            tableExists(connection, "account_characters") &&
            tableExists(connection, "realms") &&
            tableExists(connection, "worlds")
}
