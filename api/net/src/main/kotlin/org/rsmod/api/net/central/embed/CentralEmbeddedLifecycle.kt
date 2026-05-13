package org.rsmod.api.net.central.embed

import com.github.michaelbull.logging.InlineLogger
import dev.or2.central.util.config.centralRuntimeConfigFromJdbc
import dev.or2.central.embed.OpenRuneCentralEmbeddedServer
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.db.jdbc.EmbeddedSameInstancePostgres
import org.rsmod.api.server.config.SameInstanceCentralConfigValidation
import org.rsmod.api.server.config.ServerConfig

@Singleton
public class CentralEmbeddedLifecycle
@Inject
constructor(
    private val serverConfig: ServerConfig,
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

        val runtime =
            centralRuntimeConfigFromJdbc(
                jdbcUrl = jdbc,
                dbUser = dbUser,
                dbPassword = dbPassword,
                dbMaximumPoolSize = pg.poolSize,
                worldLinkPort = c.linkPort,
                worldLinkSoBacklog = 512,
            )
        logger.info {
            "Starting embedded OpenRune Central (HTTP port ${c.httpPort}, world-link ${c.linkPort}, JDBC $jdbc)"
        }
        val centralServer = OpenRuneCentralEmbeddedServer(c.httpPort, runtime)
        centralServer.start()
        server = centralServer
    }

    public fun stopIfRunning() {
        server?.stop()
        server = null
    }
}
