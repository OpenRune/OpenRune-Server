package dev.openrune.tools

import com.github.michaelbull.logging.InlineLogger
import com.google.gson.JsonParser
import dev.openrune.gamevals.GameValProvider
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Imports the latest Shortest Path tables using the selected OSRS revision's gameval names. */
internal object ShortestPathTransports {
    private val logger = InlineLogger()
    private const val REPO = "Skretzo/shortest-path"
    private const val TRANSPORTS = "src/main/resources/transports"
    private val files =
        listOf("transports.tsv", "agility_shortcuts.tsv", "teleportation_levers.tsv", "teleportation_portals.tsv")
    private val shaPattern = Regex("[0-9a-f]{40}")
    private val skillRequirement = Regex("(\\d+)\\s+(\\S+)")
    private val conditionKey = Regex("[A-Za-z0-9_:.\\-]+(?=[!<>=])")

    fun download(target: File, reportDir: File, provider: GameValProvider) {
        val client =
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        val commits = client.fetch("https://api.github.com/repos/$REPO/commits?per_page=1")
        val commit = JsonParser.parseString(commits).asJsonArray.first().asJsonObject.get("sha").asString
        require(shaPattern.matches(commit)) { "Invalid Shortest Path commit: $commit" }
        val baseUrl = "https://raw.githubusercontent.com/$REPO/$commit"

        val gamevals = TransportGamevals(provider)
        val tables = files.associateWith { file ->
            normalize(client.fetch("$baseUrl/$TRANSPORTS/$file"), gamevals)
        }
        val license = client.fetch("$baseUrl/LICENSE")

        target.mkdirs()
        tables.forEach { (file, data) -> File(target, file).writeText(data) }
        File(target, "LICENSE-shortest-path").writeText(license)
        gamevals.dumpLocs(reportDir)
        File(target, "COMMIT").writeText("$commit\n")
        if (gamevals.missingLocs.isNotEmpty()) {
            logger.warn { "Skipped Shortest Path locs without gamevals: ${gamevals.missingLocs.sorted()}" }
        }
        logger.info { "Imported Shortest Path transports ($commit) to ${target.absolutePath}" }
    }

    private fun normalize(text: String, gamevals: TransportGamevals): String {
        val lines = text.lines()
        val header = lines.first { it.startsWith("# Origin\t") }
        val columns = header.split('\t')
        val menu = columns.indexOf("menuOption menuTarget objectID")
        val skills = columns.indexOf("Skills")
        val conditions =
            mapOf("Items" to "obj", "Varbits" to "varbit", "VarPlayers" to "varp")
                .mapKeys { (column, _) -> columns.indexOf(column) }
                .filterKeys { it >= 0 }
        require(menu >= 0) { "Shortest Path table is missing the objectID column" }

        return buildString {
            for (line in lines) {
                if (line.isBlank() || line.startsWith("#")) {
                    appendLine(line.trimEnd())
                    continue
                }
                val cells = line.split('\t').toMutableList()
                val action = cells.getOrNull(menu)?.trim().orEmpty()
                val id = action.substringAfterLast(' ').toIntOrNull() ?: continue
                val loc = gamevals.name("loc", id.toString())
                if (loc == null) {
                    gamevals.missingLocs += id
                    continue
                }
                cells[menu] = "${action.substringBeforeLast(' ')} $loc"
                if (skills in cells.indices) {
                    cells[skills] = cells[skills].split(';').mapNotNull { requirement ->
                        val match = skillRequirement.matchEntire(requirement.trim()) ?: return@mapNotNull null
                        val (level, skill) = match.destructured
                        val name = gamevals.name("stat", skill) ?: return@mapNotNull null
                        "$level $name"
                    }.joinToString(";")
                }
                for ((column, table) in conditions) {
                    if (column in cells.indices) {
                        cells[column] = conditionKey.replace(cells[column]) { match ->
                            gamevals.name(table, match.value) ?: match.value
                        }
                    }
                }
                appendLine(cells.joinToString("\t").trimEnd())
            }
        }.trimEnd() + "\n"
    }

    private fun HttpClient.fetch(url: String): String {
        val request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).build()
        val response = send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "Fetching $url failed with HTTP ${response.statusCode()}" }
        return response.body()
    }

    private class TransportGamevals(private val provider: GameValProvider) {
        val missingLocs = mutableSetOf<Int>()
        private val locs = sortedMapOf<String, Int>()
        private val byId = mutableMapOf<String, Map<Int, String>>()

        fun name(table: String, input: String): String? {
            val entries = provider.mappings[table].orEmpty()
            val id = input.toIntOrNull()
            val key =
                if (id == null) {
                    "$table.${input.lowercase().removePrefix("$table.")}"
                } else {
                    byId.getOrPut(table) {
                        entries.entries.sortedByDescending { it.key }.associate { it.value to it.key }
                    }[id] ?: return null
                }
            val value = entries[key]?.takeIf { it >= 0 } ?: return null
            if (table == "loc") locs[key.removePrefix("loc.")] = value
            return key
        }

        fun dumpLocs(dir: File) {
            dir.mkdirs()
            File(dir, "shortest-path-locs.rscm").writeText(
                locs.entries.joinToString("\n", postfix = "\n") { (key, id) -> "$key=$id" },
            )
        }
    }
}
