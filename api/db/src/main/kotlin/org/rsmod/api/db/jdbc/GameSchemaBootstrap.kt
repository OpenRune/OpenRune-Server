package org.rsmod.api.db.jdbc

import com.github.michaelbull.logging.InlineLogger
import dev.or2.sql.OpenRuneSql
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.sql.Connection

internal object GameSchemaBootstrap {
    private val logger = InlineLogger()
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

    /**
     * Older installs stored persistent varps as JSON on `account_characters.varps`. New schema uses
     * `character_varps` (one row per varp id + value). Copies data then drops the legacy column.
     */
    fun migrateVarpsJsonToCharacterVarpsTable(connection: Connection) {
        if (!tableExists(connection, "account_characters")) {
            return
        }
        val columns = columnNames(connection, "account_characters")
        if (!columns.contains("varps")) {
            return
        }
        connection.createStatement().use { st ->
            st.execute(
                """
                CREATE TABLE IF NOT EXISTS character_varps (
                    character_id INTEGER NOT NULL REFERENCES account_characters (id) ON DELETE CASCADE,
                    varp_key TEXT NOT NULL,
                    value INTEGER NOT NULL,
                    PRIMARY KEY (character_id, varp_key)
                )
                """.trimIndent(),
            )
            st.execute(
                "CREATE INDEX IF NOT EXISTS idx_character_varps_character ON character_varps (character_id)",
            )
        }
        try {
            connection.createStatement().use { st ->
                st.execute(
                    """
                    INSERT INTO character_varps (character_id, varp_key, value)
                    SELECT c.id, e.key, (e.value)::int
                    FROM account_characters c
                    CROSS JOIN LATERAL jsonb_each_text(c.varps::jsonb) AS e(key, value)
                    WHERE c.varps IS NOT NULL
                        AND trim(c.varps) <> ''
                        AND trim(c.varps) <> '{}'
                    ON CONFLICT (character_id, varp_key) DO NOTHING
                    """.trimIndent(),
                )
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to migrate account_characters.varps JSON into character_varps; " +
                    "fix invalid JSON or migrate manually, then restart."
            }
            throw e
        }
        connection.createStatement().use { st ->
            st.execute("ALTER TABLE account_characters DROP COLUMN varps")
        }
        logger.info { "Migrated legacy account_characters.varps JSON into character_varps table." }
    }

    /**
     * Older installs stored persistent attrs as JSON on `account_characters.attrs`. New schema uses
     * `character_attrs` (one row per key, JSON text per value). Copies data then drops the legacy column.
     */
    fun migrateAttrsJsonToCharacterAttrsTable(connection: Connection) {
        if (!tableExists(connection, "account_characters")) {
            return
        }
        val columns = columnNames(connection, "account_characters")
        if (!columns.contains("attrs")) {
            return
        }
        connection.createStatement().use { st ->
            st.execute(
                """
                CREATE TABLE IF NOT EXISTS character_attrs (
                    character_id INTEGER NOT NULL REFERENCES account_characters (id) ON DELETE CASCADE,
                    attr_key TEXT NOT NULL,
                    value_json TEXT NOT NULL,
                    PRIMARY KEY (character_id, attr_key)
                )
                """.trimIndent(),
            )
            st.execute(
                "CREATE INDEX IF NOT EXISTS idx_character_attrs_character ON character_attrs (character_id)",
            )
        }
        try {
            connection.createStatement().use { st ->
                st.execute(
                    """
                    INSERT INTO character_attrs (character_id, attr_key, value_json)
                    SELECT c.id, e.key, e.value::text
                    FROM account_characters c
                    CROSS JOIN LATERAL jsonb_each(c.attrs::jsonb) AS e(key, value)
                    WHERE c.attrs IS NOT NULL
                        AND trim(c.attrs) <> ''
                        AND trim(c.attrs) <> '{}'
                    ON CONFLICT (character_id, attr_key) DO NOTHING
                    """.trimIndent(),
                )
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to migrate account_characters.attrs JSON into character_attrs; " +
                    "fix invalid JSON or migrate manually, then restart."
            }
            throw e
        }
        connection.createStatement().use { st ->
            st.execute("ALTER TABLE account_characters DROP COLUMN attrs")
        }
        logger.info { "Migrated legacy account_characters.attrs JSON into character_attrs table." }
    }

    /**
     * Older `character_varps` used integer column `varp_id`. Renames to `varp_key` and stores text
     * (numeric string is fine; next save rewrites to `varp.*` keys).
     */
    fun migrateCharacterVarpsVarpIdToVarpKey(connection: Connection) {
        if (!tableExists(connection, "character_varps")) {
            return
        }
        val cols = columnNames(connection, "character_varps")
        if (!cols.contains("varp_id")) {
            return
        }
        connection.createStatement().use { st ->
            st.execute("ALTER TABLE character_varps RENAME COLUMN varp_id TO varp_key")
            st.execute("ALTER TABLE character_varps ALTER COLUMN varp_key TYPE TEXT USING varp_key::text")
        }
        logger.info { "Migrated character_varps.varp_id to varp_key TEXT." }
    }

    /** Legacy inventories / inventory_objs used integer ids; convert to TEXT for RSCM string keys. */
    fun migrateInventoryStringKeysFromInts(connection: Connection) {
        if (tableExists(connection, "inventories")) {
            val invType = columnDataType(connection, "inventories", "inv_type")
            if (invType != null && invType.equals("integer", ignoreCase = true)) {
                connection.createStatement().use { st ->
                    st.execute(
                        "ALTER TABLE inventories ALTER COLUMN inv_type TYPE TEXT USING inv_type::text",
                    )
                }
                logger.info { "Migrated inventories.inv_type to TEXT." }
            }
        }
        if (tableExists(connection, "inventory_objs")) {
            val objType = columnDataType(connection, "inventory_objs", "obj")
            if (objType != null && objType.equals("integer", ignoreCase = true)) {
                connection.createStatement().use { st ->
                    st.execute(
                        "ALTER TABLE inventory_objs ALTER COLUMN obj TYPE TEXT USING obj::text",
                    )
                }
                logger.info { "Migrated inventory_objs.obj to TEXT." }
            }
        }
    }

    /**
     * Legacy `inventories.id` / `inventory_objs.inventory_id` were INTEGER surrogate keys, later TEXT
     * composite. Current bundled schema uses PK (`character_id`, `inv`) and matching `inventory_objs`;
     * this migration only runs when an `id` column still exists.
     */
    fun migrateInventoryRowIdsToTextComposite(connection: Connection) {
        if (!tableExists(connection, "inventories") || !tableExists(connection, "inventory_objs")) {
            return
        }
        val invColumns = columnNames(connection, "inventories")
        if (!invColumns.contains("id")) {
            return
        }
        val idType = columnDataType(connection, "inventories", "id") ?: return
        if (!idType.equals("integer", ignoreCase = true) && !idType.equals("bigint", ignoreCase = true)) {
            return
        }
        connection.createStatement().use { st ->
            st.execute("ALTER TABLE inventory_objs DROP CONSTRAINT IF EXISTS inventory_objs_inventory_id_fkey")
            st.execute("ALTER TABLE inventories DROP CONSTRAINT IF EXISTS inventories_pkey")
            try {
                st.execute("ALTER TABLE inventories ALTER COLUMN id DROP IDENTITY IF EXISTS")
            } catch (_: Exception) {
                // serial/identity may not exist on older dumps
            }
            st.execute(
                """
                ALTER TABLE inventory_objs ALTER COLUMN inventory_id TYPE TEXT USING (
                    (SELECT i.character_id::text || '|' || i.inv_type::text FROM inventories i WHERE i.id = inventory_objs.inventory_id)
                )
                """.trimIndent(),
            )
            st.execute(
                "ALTER TABLE inventories ALTER COLUMN id TYPE TEXT USING (character_id::text || '|' || inv_type::text)",
            )
            st.execute("ALTER TABLE inventories ADD PRIMARY KEY (id)")
            st.execute(
                """
                ALTER TABLE inventory_objs ADD CONSTRAINT inventory_objs_inventory_id_fkey
                    FOREIGN KEY (inventory_id) REFERENCES inventories (id) ON DELETE CASCADE
                """.trimIndent(),
            )
        }
        logger.info { "Migrated inventories.id / inventory_objs.inventory_id to TEXT composite keys." }
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
