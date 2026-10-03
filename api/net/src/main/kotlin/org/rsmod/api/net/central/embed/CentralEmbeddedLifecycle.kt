package org.rsmod.api.net.central.embed

import dev.or2.central.auth.PasswordAuthConfig
import dev.or2.central.embed.OpenRuneCentralEmbeddedServer
import dev.or2.central.util.config.centralRuntimeConfigFromJdbc
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.db.jdbc.EmbeddedSameInstancePostgres
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
        startCentralPreservingDatabase(centralServer::start, centralServer::stop)
        server = centralServer
    }

    public fun stopIfRunning() {
        server?.stop()
        server = null
    }
}

internal fun startCentralPreservingDatabase(start: () -> Unit, stop: () -> Unit) {
    try {
        start()
    } catch (failure: Throwable) {
        runCatching(stop).exceptionOrNull()?.let { stopFailure ->
            if (stopFailure !== failure) failure.addSuppressed(stopFailure)
        }
        throw IllegalStateException(
            "OpenRune Central could not start. No automatic database reset was attempted. " +
                "Inspect the startup cause and back up the database before repairing or migrating its schema.",
            failure,
        )
    }
}
