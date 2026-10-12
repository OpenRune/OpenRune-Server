package org.rsmod.api.testing.persistence

import com.github.michaelbull.logging.InlineLogger
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres
import jakarta.inject.Provider
import java.sql.Connection
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.measureTime
import org.rsmod.api.db.DatabaseConfig
import org.rsmod.api.db.jdbc.GameConnection
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.world.WorldType

internal class PersistencePostgres private constructor(private val postgres: EmbeddedPostgres) :
    AutoCloseable {
    private val logger = InlineLogger()

    private val admin: Connection =
        postgres.postgresDatabase.connection.apply { autoCommit = true }

    private val cloneCount = AtomicInteger()

    private fun createSeed() {
        execute("CREATE DATABASE $SEED_DATABASE")
        val seed = GameDatabase(Provider { config(SEED_DATABASE) }, Provider { SERVER_CONFIG })
        seed.connect(GameConnection(Provider { config(SEED_DATABASE) }))
        seed.close()
    }

    fun cloneSeed(): ClonedDatabase {
        val name = "test_${cloneCount.incrementAndGet()}"
        val elapsed =
            synchronized(admin) {
                measureTime { execute("CREATE DATABASE $name TEMPLATE $SEED_DATABASE") }
            }
        val database = GameDatabase(Provider { config(name) }, Provider { SERVER_CONFIG })
        val connected = measureTime { database.connect(GameConnection(Provider { config(name) })) }
        logger.info { "Cloned '$SEED_DATABASE' into '$name' in $elapsed, connected in $connected." }
        return ClonedDatabase(name, database)
    }

    private fun drop(name: String) {
        execute("DROP DATABASE IF EXISTS $name WITH (FORCE)")
    }

    private fun execute(sql: String) {
        synchronized(admin) { admin.createStatement().use { it.execute(sql) } }
    }

    private fun config(database: String): DatabaseConfig =
        DatabaseConfig(
            jdbcUrl = postgres.getJdbcUrl(USER, database),
            user = USER,
            password = "",
            usesEmbeddedPostgres = false,
        )

    override fun close() {
        admin.close()
        postgres.close()
    }

    inner class ClonedDatabase(private val name: String, val database: GameDatabase) :
        AutoCloseable {
        override fun close() {
            try {
                database.close()
            } finally {
                drop(name)
            }
        }
    }

    companion object {
        private const val USER = "postgres"
        private const val SEED_DATABASE = "seed"

        private val SERVER_CONFIG =
            ServerConfig(
                name = "persistence-test",
                gamePort = 0,
                revision = 0,
                environment = "test",
                world = 1,
                worldTypes = WorldType.entries.map(WorldType::key),
            )

        fun start(): PersistencePostgres {
            val postgres =
                EmbeddedPostgres.builder()
                    .setServerConfig("fsync", "off")
                    .setServerConfig("synchronous_commit", "off")
                    .setServerConfig("full_page_writes", "off")
                    .start()
            return PersistencePostgres(postgres).apply { createSeed() }
        }
    }
}
