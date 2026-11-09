package dev.openrune.tools

import com.github.michaelbull.logging.InlineLogger
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Downloads the Shortest Path RuneLite plugin's transport tables (BSD 2-Clause,
 * https://github.com/Skretzo/shortest-path) into `.data/transports/`, where the map-transports
 * plugin reads them. Pinned to [COMMIT] so every install serves the same rows.
 */
public object ShortestPathTransports {
    private val logger = InlineLogger()

    public const val COMMIT: String = "af1dbb918c408bbd0cc26d7ec3d6469192e3cb07"

    public val FILES: List<String> =
        listOf(
            "transports.tsv",
            "agility_shortcuts.tsv",
            "teleportation_levers.tsv",
            "teleportation_portals.tsv",
        )

    private const val BASE_URL =
        "https://raw.githubusercontent.com/Skretzo/shortest-path/$COMMIT/src/main/resources/transports"

    public fun download(target: File) {
        target.mkdirs()
        val client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        for (file in FILES) {
            val request = HttpRequest.newBuilder(URI.create("$BASE_URL/$file")).GET().build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofInputStream())
            if (response.statusCode() != 200) {
                response.body().close()
                error("Downloading $file from Shortest Path failed: HTTP ${response.statusCode()}")
            }
            val temp = File(target, "$file.part")
            response.body().use { Files.copy(it, temp.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            Files.move(
                temp.toPath(),
                File(target, file).toPath(),
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }
        File(target, "COMMIT").writeText("$COMMIT\n")
        logger.info { "Downloaded Shortest Path transports ($COMMIT) to ${target.absolutePath}" }
    }
}
