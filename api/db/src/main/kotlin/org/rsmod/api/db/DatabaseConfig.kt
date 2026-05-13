package org.rsmod.api.db

import org.rsmod.api.db.jdbc.EmbeddedSameInstancePostgres
import org.rsmod.api.server.config.ServerConfig

public data class DatabaseConfig(
    public val jdbcUrl: String,
    public val user: String,
    public val password: String,
) {
    public companion object {
        public fun create(serverConfig: ServerConfig): DatabaseConfig {
            val embedded = EmbeddedSameInstancePostgres.jdbcTripleIfEmbedded()
            val dbPg = serverConfig.database?.postgres
            val gameJdbc = dbPg?.jdbcUrl?.trim().orEmpty()

            val gameJdbcOverridesEmbedded =
                gameJdbc.isNotEmpty() && !isStockTemplateLocalGameJdbc(gameJdbc)

            if (embedded != null && !gameJdbcOverridesEmbedded) {
                return fromBase(embedded)
            }

            if (gameJdbc.isNotEmpty()) {
                val pg = checkNotNull(dbPg) { "database.postgres required when jdbc-url is set" }
                return fromBase(
                    Triple(
                        gameJdbc,
                        pg.user.trim().ifBlank { "openrune" },
                        pg.password,
                    ),
                )
            }

            embedded?.let {
                return fromBase(it)
            }

            val centralPg = serverConfig.central?.postgres
            val (baseJdbc, baseUser, basePassword) =
                when {
                    !centralPg?.jdbcUrl.isNullOrBlank() ->
                        Triple(
                            centralPg.jdbcUrl.trim(),
                            centralPg.user.trim().ifBlank { "openrune" },
                            centralPg.password,
                        )
                    else ->
                        Triple(
                            "jdbc:postgresql://127.0.0.1:5432/openrune_game",
                            "openrune",
                            "openrune",
                        )
                }
            return fromBase(Triple(baseJdbc, baseUser, basePassword))
        }

        private fun isStockTemplateLocalGameJdbc(jdbcUrl: String): Boolean =
            jdbcUrl.equals(STOCK_GAME_JDBC_127, ignoreCase = true) ||
                jdbcUrl.equals(STOCK_GAME_JDBC_LOCALHOST, ignoreCase = true)

        private const val STOCK_GAME_JDBC_127 = "jdbc:postgresql://127.0.0.1:5432/openrune_game"
        private const val STOCK_GAME_JDBC_LOCALHOST = "jdbc:postgresql://localhost:5432/openrune_game"

        private fun fromBase(base: Triple<String, String, String>): DatabaseConfig {
            val (baseJdbc, baseUser, basePassword) = base
            val jdbcUrl =
                System.getenv("OPENRUNE_JDBC_URL")?.trim()?.takeIf { it.isNotBlank() } ?: baseJdbc
            val user =
                System.getenv("OPENRUNE_DB_USER")?.trim()?.takeIf { it.isNotBlank() } ?: baseUser
            val password =
                System.getenv("OPENRUNE_DB_PASSWORD")?.trim() ?: basePassword
            return DatabaseConfig(
                jdbcUrl = jdbcUrl,
                user = user,
                password = password,
            )
        }
    }
}
