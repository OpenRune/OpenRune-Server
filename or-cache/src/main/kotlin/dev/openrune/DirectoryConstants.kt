package dev.openrune

import java.nio.file.Path
import java.nio.file.Paths

object DirectoryConstants {
    val DATA_PATH: Path = Paths.get(".data")
    val CACHE_PATH: Path = DATA_PATH.resolve("cache")

    val CS2_PATH: Path = userAppDataDir().resolve(projectAppName()).resolve("cs2")

    fun projectAppName(): String {
        val file =
            listOf("game.yml", "../game.yml", "game.example.yml", "../game.example.yml")
                .map { Paths.get(it).toFile() }
                .firstOrNull { it.exists() }

        val raw =
            file
                ?.readLines()
                ?.firstOrNull { it.trimStart().startsWith("name:") }
                ?.substringAfter("name:")
                ?.trim()
                ?.removeSurrounding("\"")
                ?.removeSurrounding("'")
                ?.trim()
                .orEmpty()

        val sanitized =
            raw.replace(Regex("""[<>:"/\\|?*\u0000-\u001F]"""), "_")
                .trim()
                .trimStart('.')
                .trimEnd('.')

        return sanitized.ifBlank { "OpenRune" }
    }

    fun userAppDataDir(): Path {
        val os = System.getProperty("os.name").lowercase()
        val home = Paths.get(System.getProperty("user.home"))
        return when {
            os.contains("win") -> {
                val local = System.getenv("LOCALAPPDATA")
                val roaming = System.getenv("APPDATA")
                when {
                    !local.isNullOrBlank() -> Paths.get(local)
                    !roaming.isNullOrBlank() -> Paths.get(roaming)
                    else -> home.resolve("AppData").resolve("Local")
                }
            }
            os.contains("mac") -> home.resolve("Library").resolve("Application Support")
            else -> {
                val xdg = System.getenv("XDG_DATA_HOME")
                if (!xdg.isNullOrBlank()) Paths.get(xdg) else home.resolve(".local").resolve("share")
            }
        }
    }
}
