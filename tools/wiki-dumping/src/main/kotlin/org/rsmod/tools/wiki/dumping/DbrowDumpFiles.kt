package org.rsmod.tools.wiki.dumping

import java.nio.file.Files
import java.nio.file.Path

object DbrowDumpFiles {
    data class LoadedDump(
        val text: String,
        val source: Path,
    )

    fun requireLocal(rootDir: String?, explicitPath: String? = null): LoadedDump {
        explicitPath?.let { path ->
            val file = Path.of(path)
            if (Files.isRegularFile(file)) {
                return LoadedDump(text = Files.readString(file), source = file)
            }
            error("dump.dbrow not found: $file")
        }

        return readLocal(rootDir)
            ?: error(
                "dump.dbrow not found. Place it at .data/osrs-dumps/dump.dbrow " +
                    "or pass --dbrow=/path/to/dump.dbrow",
            )
    }

    fun readLocal(rootDir: String?): LoadedDump? {
        val path = localPath(rootDir) ?: return null
        if (!Files.isRegularFile(path)) {
            return null
        }
        return LoadedDump(text = Files.readString(path), source = path)
    }

    fun localPath(rootDir: String?): Path? {
        for (candidate in candidatePaths(rootDir)) {
            if (Files.isRegularFile(candidate)) {
                return candidate
            }
        }
        return null
    }

    private fun candidatePaths(rootDir: String?): List<Path> =
        buildList {
            if (!rootDir.isNullOrBlank()) {
                val root = Path.of(rootDir)
                add(root.resolve(".data/osrs-dumps/dump.dbrow"))
                add(root.resolve(".data/raw-cache/dump.dbrow"))
            }
            var cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
            repeat(8) {
                add(cursor.resolve(".data/osrs-dumps/dump.dbrow"))
                add(cursor.resolve(".data/raw-cache/dump.dbrow"))
                cursor = cursor.parent ?: return@repeat
            }
        }
}
