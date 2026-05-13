package org.rsmod.api.db.jdbc

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

/**
 * Cleans up a previous embedded PostgreSQL instance that outlived the JVM (e.g. SIGKILL), so a new
 * [io.zonky.test.db.postgres.embedded.EmbeddedPostgres] can bind the port and reuse PGDATA safely.
 */
internal object EmbeddedPostgresStaleRecovery {
    internal fun recoverIfNeeded(dataDir: Path) {
        val absoluteDataDir = dataDir.toAbsolutePath().normalize()
        val pidFile = absoluteDataDir.resolve("postmaster.pid")
        if (!Files.isRegularFile(pidFile)) {
            return
        }
        val lines =
            runCatching { Files.readAllLines(pidFile, StandardCharsets.UTF_8) }.getOrNull()
                ?: return
        if (lines.size < 2) {
            deletePidFile(pidFile)
            return
        }
        val pid = lines[0].trim().toLongOrNull()
        if (pid == null) {
            deletePidFile(pidFile)
            return
        }
        val listedDataDir =
            runCatching { Paths.get(lines[1].trim()).toAbsolutePath().normalize() }.getOrNull()
                ?: run {
                    deletePidFile(pidFile)
                    return
                }
        if (!pathsEqualForEmbeddedOs(absoluteDataDir, listedDataDir)) {
            return
        }
        val handle = ProcessHandle.of(pid)
        if (handle.isEmpty || !handle.get().isAlive) {
            deletePidFile(pidFile)
            return
        }
        val postmaster = handle.get()
        stopPostgresProcessTree(postmaster)
        deletePidFile(pidFile)
    }

    private fun stopPostgresProcessTree(root: ProcessHandle) {
        val descendants = buildList {
            root.descendants().forEach { add(it) }
        }
        for (child in descendants) {
            runCatching { child.destroyForcibly() }
        }
        runCatching { root.destroyForcibly() }
        val deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(8)
        while (System.nanoTime() < deadlineNanos && root.isAlive) {
            try {
                Thread.sleep(50)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                break
            }
        }
        if (!root.isAlive) {
            try {
                Thread.sleep(200)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    private fun deletePidFile(pidFile: Path) {
        runCatching { Files.deleteIfExists(pidFile) }
    }

    private fun pathsEqualForEmbeddedOs(a: Path, b: Path): Boolean {
        val na = a.normalize().toString()
        val nb = b.normalize().toString()
        return na.equals(nb, ignoreCase = true)
    }
}
