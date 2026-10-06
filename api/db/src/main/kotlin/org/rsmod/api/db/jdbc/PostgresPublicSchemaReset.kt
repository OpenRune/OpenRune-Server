package org.rsmod.api.db.jdbc

import java.sql.Connection

/** Dev recovery: remove every object in `public`. Caller should use autocommit or commit after. */
public object PostgresPublicSchemaReset {
    public fun dropAllInPublicSchema(connection: Connection) {
        dropWorldTypeSchemas(connection)
        connection.createStatement().use { st ->
            st.execute("DROP SCHEMA public CASCADE")
            st.execute("CREATE SCHEMA public")
            st.execute("GRANT ALL ON SCHEMA public TO postgres")
            st.execute("GRANT ALL ON SCHEMA public TO public")
        }
    }

    /** Dropping only `public` would leave per-mode saves behind, orphaned from their accounts. */
    private fun dropWorldTypeSchemas(connection: Connection) {
        val schemas =
            connection.createStatement().use { st ->
                st
                    .executeQuery(
                        """
                        SELECT table_schema
                        FROM information_schema.tables
                        WHERE table_name = 'character_progress'
                          AND table_schema <> 'public'
                        """.trimIndent(),
                    ).use { rs ->
                        buildList {
                            while (rs.next()) {
                                val name = rs.getString("table_schema") ?: continue
                                if (SCHEMA_NAME_PATTERN.matches(name)) {
                                    add(name)
                                }
                            }
                        }
                    }
            }
        connection.createStatement().use { st ->
            for (schema in schemas) {
                st.execute("DROP SCHEMA IF EXISTS \"$schema\" CASCADE")
            }
        }
    }

    private val SCHEMA_NAME_PATTERN = Regex("^[a-z][a-z0-9_]{0,62}$")
}
