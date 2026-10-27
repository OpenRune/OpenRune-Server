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
    @JsonProperty("level-up-popups")
    val levelUpPopups: Boolean = false,
    @JsonProperty("idle-logout")
    val idleLogout: Boolean = true,
    @JsonProperty("home-teleport-cooldown")
    val homeTeleportCooldown: Int = 30,
    val gravestones: GravestonesYaml = GravestonesYaml(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
public data class GravestonesYaml(
    val enabled: Boolean = true,
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
    @JsonProperty("websocket") val webSocket: WebSocketBridgeConfig = WebSocketBridgeConfig(),
)

/**
 * Bridge that lets a browser webclient (which only speaks WebSocket) reach this server's
 * plain-TCP [ServerConfig.gamePort]. See `WebSocketBridgeService` in `api:net` for what actually
 * listens on [port] — this is just its config. [tls] is unset (no TLS) by default, matching local
 * dev served over plain `http://`, where browsers don't require `wss://`.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class WebSocketBridgeConfig(
    val enabled: Boolean = true,
    val port: Int = 8091,
    val host: String = "0.0.0.0",
    val tls: WebSocketTlsConfig? = null,
)

/**
 * Enables `wss://` on the bridge — required the moment a *production* ClientLabs host points at
 * this server (browsers block an `https://` page from opening a plain `ws://` connection —
 * "mixed content"). Local dev, served over plain `http://`, never needs this.
 *
 * Two ways to get a certificate — pick one:
 *  - Leave [certPath]/[keyPath] unset and set [domain] + [email]: a certificate is obtained and
 *    renewed automatically from the ACME CA at [caDirectoryUrl] (Let's Encrypt by default) via
 *    the HTTP-01 challenge, which briefly needs port 80 reachable from the public internet during
 *    each check — nothing else has to run there, and it's only bound for the duration of a check.
 *  - Or set [certPath]/[keyPath] to your own PEM certificate chain + unencrypted PKCS#8 private
 *    key, if you already have a certificate or can't expose port 80.
 *
 * See `WebSocketTlsManager`/`AcmeCertificateProvisioner` in `api:net` for what actually does this.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public data class WebSocketTlsConfig(
    val enabled: Boolean = false,
    val domain: String? = null,
    val email: String? = null,
    @JsonProperty("cert-path") val certPath: String? = null,
    @JsonProperty("key-path") val keyPath: String? = null,
    @JsonProperty("cert-dir") val certDir: String = ".data/tls",
    @JsonProperty("ca-directory-url") val caDirectoryUrl: String = "acme://letsencrypt.org",
)

/** Masks a secret for `toString()`; an empty value stays visible so "unset" is still obvious. */
private fun redactSecret(value: String): String = if (value.isEmpty()) "" else "***"

private val JDBC_PASSWORD_PARAM = Regex("(?i)(password=)[^&;]*")

/** JDBC URLs may carry credentials as a query parameter (`...?user=x&password=y`). */
private fun redactJdbcUrl(url: String): String = url.replace(JDBC_PASSWORD_PARAM, "$1***")
