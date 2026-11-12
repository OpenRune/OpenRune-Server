package org.rsmod.api.db.jdbc

import java.nio.file.Files
import java.nio.file.Path

/**
 * A `postmaster.pid` copied along with a data directory - when the server folder is moved or
 * cloned while its database runs - names the directory it was written for. Postgres treats the copy
 * as a live lock, refuses to start and cannot be reused either, because that postmaster serves the
 * other directory. A lock naming a different directory can never belong to a server on this one, so
 * it is safe to remove.
 */
internal object ForeignPostmasterPid {
    private const val PID_FILE = "postmaster.pid"
    private const val DATA_DIR_LINE = 1

    /** Deletes [dataDir]'s lock file when it names another directory; returns that directory. */
    fun clearIfForeign(dataDir: Path): String? {
        val pidFile = dataDir.resolve(PID_FILE)
        if (!Files.isRegularFile(pidFile)) return null
        val recorded = Files.readAllLines(pidFile).getOrNull(DATA_DIR_LINE)?.trim()
        if (recorded.isNullOrEmpty() || sameDirectory(recorded, dataDir)) return null
        Files.deleteIfExists(pidFile)
        return recorded
    }

    private fun sameDirectory(recorded: String, dataDir: Path): Boolean =
        normalise(recorded) == normalise(dataDir.toAbsolutePath().normalize().toString())

    private fun normalise(path: String): String {
        val slashes = path.replace('\\', '/').trimEnd('/')
        val windows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
        return if (windows) slashes.lowercase() else slashes
    }
}
