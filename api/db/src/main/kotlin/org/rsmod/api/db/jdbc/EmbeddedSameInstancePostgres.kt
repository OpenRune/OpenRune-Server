package org.rsmod.api.db.jdbc

import dev.or2.central.db.embedded.EmbeddedPostgresSupport
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import java.net.BindException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.rsmod.api.server.config.CentralPostgresYaml
import org.rsmod.api.server.config.ServerConfig

public object EmbeddedSameInstancePostgres {
    @Volatile
    private var embedded: EmbeddedPostgres? = null

    @Volatile
    private var reused: EmbeddedPostgresSupport.Session? = null

    @Volatile
    private var activeDataDir: Path? = null

    /** Survives mid-stop clearing of [activeDataDir] so a hung close can still be force-killed. */
    @Volatile
    private var lastKnownDataDir: Path? = null

    /**
     * Starts embedded PostgreSQL when same-instance Central is enabled and no JDBC URL is set.
     * Reuses an existing postmaster for the same PGDATA when the port is still bound.
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
            if (embedded != null || reused != null) {
                return
            }
            val dataDir = embeddedPgdataDirectory(pg)
            Files.createDirectories(dataDir)
            activeDataDir = dataDir
            lastKnownDataDir = dataDir
            when (val plan = EmbeddedPostgresSupport.planStart(dataDir)) {
                is EmbeddedPostgresSupport.StartPlan.Reuse -> {
                    reused = plan.session
                }
                EmbeddedPostgresSupport.StartPlan.StartNew -> {
                    val instance = startNewEmbedded(dataDir)
                    embedded = instance
                }
            }
        }
    }

    /** JDBC URL / user / password when embedded instance is running; otherwise `null`. */
    public fun jdbcTripleIfEmbedded(): Triple<String, String, String>? {
        embedded?.let { instance ->
            return Triple(instance.getJdbcUrl("postgres", "postgres"), "postgres", "")
        }
        reused?.let { session ->
            return Triple(session.jdbcUrl, session.user, session.password)
        }
        return null
    }

    /**
     * Stops the owned embedded instance (if any) and ensures the postmaster for [activeDataDir] is
     * down. Caps wait at [timeoutMs]; on timeout, force-kills the postmaster so IntelliJ Stop cannot
     * hang indefinitely on `EmbeddedPostgres.close()`.
     */
    public fun stop(timeoutMs: Long = DEFAULT_STOP_TIMEOUT_MS) {
        val owned: EmbeddedPostgres?
        val dataDir: Path?
        synchronized(this) {
            owned = embedded
            embedded = null
            reused = null
            dataDir = activeDataDir
            activeDataDir = null
        }
        if (owned == null && dataDir == null) {
            return
        }
        val done = CountDownLatch(1)
        Thread(
                {
                    try {
                        runCatching { owned?.close() }
                        if (dataDir != null) {
                            EmbeddedPostgresSupport.ensureStopped(dataDir)
                        }
                    } finally {
                        done.countDown()
                    }
                },
                "embedded-pg-stop",
            )
            .apply {
                isDaemon = true
                start()
            }
        if (!done.await(timeoutMs, TimeUnit.MILLISECONDS) && dataDir != null) {
            runCatching { EmbeddedPostgresSupport.forceStop(dataDir) }
        }
    }

    /** Best-effort kill of any postmaster still bound to the last known data dir. */
    public fun forceStopNow() {
        val dataDir =
            synchronized(this) {
                embedded = null
                reused = null
                activeDataDir = null
                lastKnownDataDir
            }
        if (dataDir != null) {
            runCatching { EmbeddedPostgresSupport.forceStop(dataDir) }
        }
    }

    private fun startNewEmbedded(dataDir: Path): EmbeddedPostgres {
        try {
            return EmbeddedPostgres.builder()
                .setDataDirectory(dataDir.toFile())
                .setCleanDataDirectory(false)
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()
        } catch (t: Throwable) {
            if (!isBindException(t)) {
                throw t
            }
            EmbeddedPostgresSupport.forceStop(dataDir)
            return EmbeddedPostgres.builder()
                .setDataDirectory(dataDir.toFile())
                .setCleanDataDirectory(false)
                .setPGStartupWait(Duration.ofSeconds(30))
                .start()
        }
    }

    private fun isBindException(t: Throwable): Boolean {
        var current: Throwable? = t
        while (current != null) {
            if (current is BindException) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun embeddedPgdataDirectory(pg: CentralPostgresYaml): Path {
        val raw = pg.embeddedPgdataDir.trim()
        val pathStr = if (raw.isEmpty()) ".data/postgres" else raw
        return Path.of(pathStr).toAbsolutePath().normalize()
    }

    private const val DEFAULT_STOP_TIMEOUT_MS = 5_000L
}
