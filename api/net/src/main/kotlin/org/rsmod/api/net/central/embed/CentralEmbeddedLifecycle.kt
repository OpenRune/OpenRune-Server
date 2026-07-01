package org.rsmod.api.net.central.embed

import com.github.michaelbull.logging.InlineLogger
import dev.or2.central.util.config.centralRuntimeConfigFromJdbc
import dev.or2.central.embed.OpenRuneCentralEmbeddedServer
import dev.or2.central.auth.PasswordAuthConfig
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.sql.DriverManager
import org.rsmod.api.db.jdbc.EmbeddedSameInstancePostgres
import org.rsmod.api.db.jdbc.PostgresPublicSchemaReset
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.server.config.SameInstanceCentralConfigValidation
import org.rsmod.api.server.config.ServerConfig

@Singleton
public class CentralEmbeddedLifecycle
@Inject
constructor(
    private val serverConfig: ServerConfig,
    private val openRuneCentral: OpenRuneCentralWorldLink,
) {
    private val logger = InlineLogger()
    private var server: OpenRuneCentralEmbeddedServer? = null

    public fun startIfConfigured() {
        val c = serverConfig.central ?: return
        if (!c.sameInstance) {
            return
        }
        val pg = c.postgres ?: error(SameInstanceCentralConfigValidation.missingPostgresMessage())

        val jdbcFromYaml = pg.jdbcUrl.trim()
        val (jdbc, dbUser, dbPassword) =
            if (jdbcFromYaml.isNotEmpty()) {
                Triple(
                    jdbcFromYaml,
                    pg.user.trim().ifBlank { "openrune" },
                    pg.password,
                )
            } else {
                val embeddedCreds =
                    EmbeddedSameInstancePostgres.jdbcTripleIfEmbedded()
                        ?: error(
                            "game.yml: `central.postgres.jdbc-url` is blank but embedded PostgreSQL did not start. " +
                                "Ensure [org.rsmod.server.app.GameBootstrap] calls " +
                                "`EmbeddedSameInstancePostgres.ensureStarted` before starting embedded Central.",
                        )
                embeddedCreds
            }

        val usesEmbeddedJdbc = jdbcFromYaml.isEmpty()

        fun buildRuntime() =
            centralRuntimeConfigFromJdbc(
                jdbcUrl = jdbc,
                dbUser = dbUser,
                dbPassword = dbPassword,
                dbMaximumPoolSize = pg.poolSize,
                worldLinkPort = c.linkPort,
                httpPort = c.httpPort,
                serverName = serverConfig.name,
                worldLinkSoBacklog = 512,
                loginTimingLogs = serverConfig.loginTimingLogs,
                socialPmTraceLogs = serverConfig.socialPmTraceLogs,
            )

        val runtime = buildRuntime()
        openRuneCentral.applyPasswordAuth(
            PasswordAuthConfig(
                passwordHasher = runtime.auth.passwordHasher,
                bcryptCost = runtime.auth.bcryptCost,
                argon2Iterations = runtime.auth.argon2Iterations,
                argon2MemoryKib = runtime.auth.argon2MemoryKib,
            ),
        )

        val centralServer = OpenRuneCentralEmbeddedServer(c.httpPort, runtime)
        try {
            centralServer.start()
            server = centralServer
        } catch (t: Throwable) {
            runCatching { centralServer.stop() }
            if (!usesEmbeddedJdbc) {
                throw t
            }
            logger.warn(t) { "Embedded OpenRune Central failed to start." }
            runCatching {
                DriverManager.getConnection(jdbc, dbUser, dbPassword).use { conn ->
                    conn.autoCommit = true
                    PostgresPublicSchemaReset.dropAllInPublicSchema(conn)
                }
            }.onFailure { dropEx ->
                logger.error(dropEx) { "Failed to reset schema `public` after Central startup failure." }
            }
            logger.error {
                "Embedded database was reset (schema `public` dropped). Please restart the server."
            }
            throw IllegalStateException(
                "OpenRune Central could not start; the embedded database was reset. Please restart the server.",
                t,
            )
        }
    }

    public fun stopIfRunning() {
        server?.stop()
        server = null
    }
}
