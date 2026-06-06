package org.rsmod.tools.wiki.dumping

import dev.openrune.gamevals.GameValProvider
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/** Loads repo gamevals into [dev.openrune.definition.constants.ConstantProvider] for [dev.openrune.rscm.RSCM]. */
object GameValLoader {
    @Volatile
    private var loaded = false

    private val lock = Any()

    fun ensureLoaded(rootDir: String? = null) {
        if (loaded) {
            return
        }
        synchronized(lock) {
            if (loaded) {
                return
            }
            GameValProvider.load(rootPrefix(rootDir))
            loaded = true
        }
    }

    fun rootPrefix(rootDir: String? = null): String {
        val root = resolveRoot(rootDir)
        val path = root.toAbsolutePath().normalize().toString()
        val separator = File.separator
        return if (path.endsWith(separator)) path else path + separator
    }

    fun resolveRootOrNull(rootDir: String? = null): Path? = runCatching { resolveRoot(rootDir) }.getOrNull()

    fun resolveRoot(rootDir: String? = null): Path {
        if (!rootDir.isNullOrBlank()) {
            return Path.of(rootDir).toAbsolutePath().normalize()
        }

        val logDir = System.getenv("LOG_DIR")?.takeIf { it.isNotBlank() }
        if (logDir != null) {
            val parent = Path.of(logDir).toAbsolutePath().normalize().parent
            if (
                parent != null &&
                Files.isRegularFile(parent.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat"))
            ) {
                return parent
            }
        }

        val envRoot = System.getenv("RSPS_ROOT")?.takeIf { it.isNotBlank() }
        if (envRoot != null) {
            val envPath = Path.of(envRoot).toAbsolutePath().normalize()
            if (Files.isRegularFile(envPath.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat"))) {
                return envPath
            }
        }

        for (candidateRoot in guessRootsFromClasspath()) {
            val candidate = candidateRoot.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat")
            if (Files.isRegularFile(candidate)) {
                return candidateRoot
            }
        }

        var cursor: Path? = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
        while (cursor != null) {
            val candidate = cursor.resolve(".data").resolve("gamevals-binary").resolve("gamevals.dat")
            if (Files.isRegularFile(candidate)) {
                return cursor
            }
            cursor = cursor.parent
        }

        throw IllegalStateException(
            "Unable to locate repository root containing '.data/gamevals-binary/gamevals.dat'. " +
                "Set RSPS_ROOT or pass 'rootDir'.",
        )
    }

    private fun guessRootsFromClasspath(): List<Path> {
        val separator = System.getProperty("path.separator")
        val classpath = System.getProperty("java.class.path").orEmpty()
        if (classpath.isBlank()) {
            return emptyList()
        }

        val roots = linkedSetOf<Path>()
        for (entry in classpath.split(separator)) {
            val path = runCatching { Path.of(entry).toAbsolutePath().normalize() }.getOrNull() ?: continue
            if (!Files.exists(path)) {
                continue
            }
            var cursor: Path? = if (Files.isRegularFile(path)) path.parent else path
            repeat(8) {
                val current = cursor ?: return@repeat
                roots.add(current)
                cursor = current.parent
            }
        }
        return roots.toList()
    }
}
