package org.rsmod.api.server.config

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
public data class OpenRuneCentralGameConfig(
    @JsonProperty("same-instance") val sameInstance: Boolean = false,
    @JsonProperty("http-port") val httpPort: Int = 8080,
    val host: String = "",
    @JsonProperty("link-port") val linkPort: Int = 9091,
    @JsonProperty("world-key") val worldKey: String = "",
    val postgres: CentralPostgresYaml? = null,
) {
    // `ServerConfig` is logged at startup; never print the world-link secret.
    override fun toString(): String =
        "OpenRuneCentralGameConfig(sameInstance=$sameInstance, httpPort=$httpPort, " +
            "host=$host, linkPort=$linkPort, worldKey=${redactSecret(worldKey)}, " +
            "postgres=$postgres)"
}

public data class CentralPostgresYaml(
    @JsonProperty("jdbc-url") val jdbcUrl: String = "",
    val user: String = "openrune",
    val password: String = "openrune",
    @JsonProperty("pool-size") val poolSize: Int = 10,
    @JsonProperty("embedded-pgdata-dir") val embeddedPgdataDir: String = ".data/postgres",
) {
    override fun toString(): String =
        "CentralPostgresYaml(jdbcUrl=${redactJdbcUrl(jdbcUrl)}, user=$user, password=${redactSecret(password)}, " +
            "poolSize=$poolSize, embeddedPgdataDir=$embeddedPgdataDir)"
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class GameDatabaseYaml(
    val postgres: PostgresDbYaml? = null,
)

public data class PostgresDbYaml(
    @JsonProperty("jdbc-url") val jdbcUrl: String = "",
    val user: String = "openrune",
    val password: String = "openrune",
) {
    override fun toString(): String =
        "PostgresDbYaml(jdbcUrl=${redactJdbcUrl(jdbcUrl)}, user=$user, password=${redactSecret(password)})"
}

@JsonIgnoreProperties(ignoreUnknown = true)
public data class GameplayConfig(
    @JsonProperty("quest-requirements")
    val questRequirements: QuestRequirementsYaml = QuestRequirementsYaml(),
    @JsonProperty("drop-rates")
    val dropRates: DropRatesYaml = DropRatesYaml(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class DropRatesYaml(
    val multiplier: Double = 1.0,
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class QuestRequirementsYaml(
    val mode: String = "assume-completed",
    @JsonProperty("virtual-completions")
    val virtualCompletions: Set<String> = emptySet(),
    @JsonProperty("virtual-lines")
    val virtualLines: Set<String> = emptySet(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ServerConfig(
    val name: String,
    @JsonProperty("game-port") val gamePort: Int,
    val revision: Int,
    val environment: String,
    val world: Int,
    /** Every mode this world serves, first one being its default. Empty means `main` only. */
    @JsonProperty("world-types") val worldTypes: List<String> = emptyList(),
    val gameplay: GameplayConfig = GameplayConfig(),
    val database: GameDatabaseYaml? = null,
    val central: OpenRuneCentralGameConfig? = null,
    @JsonProperty("login-timing-logs") val loginTimingLogs: Boolean = false,
    @JsonProperty("social-pm-trace-logs") val socialPmTraceLogs: Boolean = false,
)

/** Masks a secret for `toString()`; an empty value stays visible so "unset" is still obvious. */
private fun redactSecret(value: String): String = if (value.isEmpty()) "" else "***"

private val JDBC_PASSWORD_PARAM = Regex("(?i)(password=)[^&;]*")

/** JDBC URLs may carry credentials as a query parameter (`...?user=x&password=y`). */
private fun redactJdbcUrl(url: String): String = url.replace(JDBC_PASSWORD_PARAM, "$1***")
