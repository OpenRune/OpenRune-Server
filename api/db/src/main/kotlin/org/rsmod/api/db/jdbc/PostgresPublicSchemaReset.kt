package org.rsmod.api.db.jdbc

import java.sql.Connection

/** Dev recovery: remove every object in `public`. Caller should use autocommit or commit after. */
public object PostgresPublicSchemaReset {
    public fun dropAllInPublicSchema(connection: Connection) {
        connection.createStatement().use { st ->
            st.execute("DROP SCHEMA public CASCADE")
            st.execute("CREATE SCHEMA public")
            st.execute("GRANT ALL ON SCHEMA public TO postgres")
            st.execute("GRANT ALL ON SCHEMA public TO public")
        }
    }
}
