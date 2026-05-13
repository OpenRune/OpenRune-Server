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
)

public data class CentralPostgresYaml(
    @JsonProperty("jdbc-url") val jdbcUrl: String = "",
    val user: String = "openrune",
    val password: String = "openrune",
    @JsonProperty("pool-size") val poolSize: Int = 10,
    @JsonProperty("embedded-pgdata-dir") val embeddedPgdataDir: String = ".data/pgdata",
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class GameDatabaseYaml(
    val postgres: PostgresDbYaml? = null,
)

public data class PostgresDbYaml(
    @JsonProperty("jdbc-url") val jdbcUrl: String = "",
    val user: String = "openrune",
    val password: String = "openrune",
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class ServerConfig(
    val name: String,
    @JsonProperty("game-port") val gamePort: Int,
    val revision: Int,
    val environment: String,
    val world: Int,
    val database: GameDatabaseYaml? = null,
    val central: OpenRuneCentralGameConfig? = null,
)
