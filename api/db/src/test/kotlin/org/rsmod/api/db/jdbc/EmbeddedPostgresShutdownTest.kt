package org.rsmod.api.db.jdbc

import dev.or2.central.db.embedded.EmbeddedPostgresSupport
import java.nio.file.Files
import java.nio.file.Path
import java.sql.DriverManager
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString
import kotlin.system.exitProcess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.rsmod.api.server.config.CentralPostgresYaml
import org.rsmod.api.server.config.OpenRuneCentralGameConfig
import org.rsmod.api.server.config.ServerConfig

class EmbeddedPostgresShutdownTest {
    @Test
    fun `embedded postgres stays up while other shutdown hooks still use it`(
        @TempDir tempDir: Path
    ) {
        val dataDir = tempDir.resolve("pgdata")
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val process =
            ProcessBuilder(
                    java,
                    "-cp",
                    System.getProperty("java.class.path"),
                    EmbeddedPostgresShutdownProbe::class.java.name,
                    dataDir.absolutePathString(),
                )
                .redirectErrorStream(true)
                .start()
        try {
            val output = process.inputStream.bufferedReader().readText()
            assertTrue(process.waitFor(2, TimeUnit.MINUTES))
            assertEquals(0, process.exitValue(), output)
            assertTrue(output.contains(EmbeddedPostgresShutdownProbe.QUERY_OK), output)
            assertTrue(output.contains(EmbeddedPostgresShutdownProbe.STOPPED), output)
        } finally {
            process.destroyForcibly()
            if (Files.isDirectory(dataDir)) {
                runCatching { EmbeddedPostgresSupport.forceStop(dataDir) }
            }
        }
    }
}

object EmbeddedPostgresShutdownProbe {
    const val QUERY_OK = "probe: query after shutdown started succeeded"
    const val STOPPED = "probe: embedded postgres stopped"
    private const val HOOK_DELAY_MS = 3_000L

    @JvmStatic
    fun main(args: Array<String>) {
        val config =
            ServerConfig(
                name = "probe",
                gamePort = 0,
                revision = 241,
                environment = "test",
                world = 1,
                central =
                    OpenRuneCentralGameConfig(
                        sameInstance = true,
                        postgres = CentralPostgresYaml(embeddedPgdataDir = args[0]),
                    ),
            )
        EmbeddedSameInstancePostgres.ensureStarted(config)
        val (url, user, password) = checkNotNull(EmbeddedSameInstancePostgres.jdbcTripleIfEmbedded())
        val hook = Thread {
            Thread.sleep(HOOK_DELAY_MS)
            val result = runCatching {
                DriverManager.getConnection(url, user, password).use { connection ->
                    connection.createStatement().executeQuery("select 1").next()
                }
            }
            if (result.getOrNull() == true) {
                println(QUERY_OK)
            } else {
                println("probe: query failed: ${result.exceptionOrNull()}")
            }
            EmbeddedSameInstancePostgres.stop()
            println(STOPPED)
        }
        Runtime.getRuntime().addShutdownHook(hook)
        exitProcess(0)
    }
}
