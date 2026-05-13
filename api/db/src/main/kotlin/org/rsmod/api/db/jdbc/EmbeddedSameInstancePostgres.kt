package org.rsmod.api.db.jdbc

import com.github.michaelbull.logging.InlineLogger
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import org.rsmod.api.server.config.CentralPostgresYaml
import org.rsmod.api.server.config.ServerConfig

public object EmbeddedSameInstancePostgres {
    private val logger = InlineLogger()

    @Volatile
    private var embedded: EmbeddedPostgres? = null

    /**
     * Starts embedded PostgreSQL when same-instance Central is enabled and no JDBC URL is set.
     * No-op if already started, not applicable, or an explicit `jdbc-url` is configured.
     */
    public fun ensureStarted(config: ServerConfig) {
        val central = config.central ?: return
        if (!central.sameInstance) {
            return
        }
        val pg = central.postgres ?: return
        if (pg.jdbcUrl.trim().isNotEmpty()) {
            return
        }
        synchronized(this) {
            if (embedded != null) {
                return
            }
            val dataDir = embeddedPgdataDirectory(pg)
            Files.createDirectories(dataDir)
            EmbeddedPostgresStaleRecovery.recoverIfNeeded(dataDir)
            val instance =
                EmbeddedPostgres.builder()
                    .setDataDirectory(dataDir.toFile())
                    .setCleanDataDirectory(false)
                    .setPGStartupWait(Duration.ofSeconds(30))
                    .start()
            embedded = instance
            logger.info { "Embedded PostgreSQL started (same-instance dev, PGDATA: $dataDir)" }
        }
    }

    /** JDBC URL / user / password when embedded instance is running; otherwise `null`. */
    public fun jdbcTripleIfEmbedded(): Triple<String, String, String>? {
        val instance = embedded ?: return null
        val url = instance.getJdbcUrl("postgres", "postgres")
        return Triple(url, "postgres", "")
    }

    public fun stop() {
        synchronized(this) {
            embedded?.close()
            embedded = null
        }
    }

    private fun embeddedPgdataDirectory(pg: CentralPostgresYaml): Path {
        val raw = pg.embeddedPgdataDir.trim()
        val pathStr = if (raw.isEmpty()) ".data/pgdata" else raw
        return Path.of(pathStr).toAbsolutePath().normalize()
    }
}
