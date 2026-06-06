package org.rsmod.tools.wiki.dumping

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import java.nio.file.Files
import java.nio.file.Path

object NpcDumpFiles {
    const val DUMP_NPC_URL: String =
        "https://raw.githubusercontent.com/Joshua-F/osrs-dumps/refs/heads/master/config/dump.npc"

    data class LoadedDump(
        val text: String,
        val source: Path?,
        val downloaded: Boolean,
    )

    fun readLocal(rootDir: String?): LoadedDump? {
        val path = localPath(rootDir) ?: return null
        if (!Files.isRegularFile(path)) {
            return null
        }
        return LoadedDump(
            text = Files.readString(path),
            source = path,
            downloaded = false,
        )
    }

    fun requireLocal(rootDir: String?): LoadedDump {
        return readLocal(rootDir)
            ?: error(
                "dump.npc not found. Place it at .data/osrs-dumps/dump.npc " +
                    "or pass --fetch-dump to download once.",
            )
    }

    suspend fun readOrFetch(rootDir: String?, fetchIfMissing: Boolean): LoadedDump {
        readLocal(rootDir)?.let { return it }
        if (!fetchIfMissing) {
            error(
                "dump.npc not found under .data/osrs-dumps/ or .data/raw-cache/. " +
                    "Re-run with --fetch-dump to download it.",
            )
        }
        val text = fetchRemote()
        val target = localPath(rootDir) ?: defaultWritePath(rootDir)
        Files.createDirectories(target.parent)
        Files.writeString(target, text)
        return LoadedDump(text = text, source = target, downloaded = true)
    }

    fun localPath(rootDir: String?): Path? {
        for (candidate in candidatePaths(rootDir)) {
            if (Files.isRegularFile(candidate)) {
                return candidate
            }
        }
        return null
    }

    private fun defaultWritePath(rootDir: String?): Path {
        val root =
            rootDir?.let { Path.of(it) }
                ?: candidatePaths(null).first().parent
        return root.resolve("osrs-dumps").resolve("dump.npc")
    }

    private fun candidatePaths(rootDir: String?): List<Path> =
        buildList {
            if (!rootDir.isNullOrBlank()) {
                val root = Path.of(rootDir)
                add(root.resolve(".data/osrs-dumps/dump.npc"))
                add(root.resolve(".data/raw-cache/dump.npc"))
            }
            var cursor = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize()
            repeat(8) {
                add(cursor.resolve(".data/osrs-dumps/dump.npc"))
                add(cursor.resolve(".data/raw-cache/dump.npc"))
                cursor = cursor.parent ?: return@repeat
            }
        }

    private suspend fun fetchRemote(): String =
        HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 30_000
            }
        }.use { client ->
            client.get(DUMP_NPC_URL).bodyAsText()
        }
}
