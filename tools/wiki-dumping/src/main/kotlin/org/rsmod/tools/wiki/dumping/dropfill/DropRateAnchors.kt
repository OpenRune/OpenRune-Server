package org.rsmod.tools.wiki.dumping.dropfill

import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText

/**
 * The dumper's own markers of drops it could not rate-resolve:
 * - TOML tables carry `# Unknown wiki drop rate: <item> [<table>/<subsection>/<rarity>]`
 * - Kotlin tables carry `// - <item> [<table>/<rarity>]`
 *
 * "Mechanical" anchors hold an un-evaluated `{{#expr}}` rate template; everything else (Common,
 * Varies, refs) is editorial and left untouched.
 */
data class DropAnchor(
    val file: Path,
    val line: Int, // 1-based
    val item: String,
    val table: String,
    /** TOML anchors only; Kotlin anchors carry no subsection. */
    val subsection: String?,
    val rarity: String,
    val page: String,
) {
    val mechanical: Boolean
        get() = rarity.isMechanicalRarity
}

/** A `_unknown_drop_rates.txt` row: wikiPage, item, section, subsection, rarity. */
data class ManifestRow(
    val page: String,
    val item: String,
    val section: String,
    val subsection: String,
    val rarity: String,
) {
    val mechanical: Boolean
        get() = rarity.isMechanicalRarity
}

private val String.isMechanicalRarity: Boolean
    get() = contains("{{#expr")

object DropRateAnchors {
    private val TOML_ANCHOR =
        Regex("""^# Unknown wiki drop rate: (.+?) \[([^/]+)/([^/]+)/(.*)]\s*$""")
    private val KT_ANCHOR = Regex("""^//   - (.+?) \[([^/]+)/(.*)]\s*$""")
    private val TOML_ID = Regex("""^id = "(.+)"$""", RegexOption.MULTILINE)
    private val KT_IDENTIFIER = Regex("""tableIdentifier = "(.+?)"""")

    fun parseManifest(text: String): List<ManifestRow> =
        text
            .lineSequence()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split("\t")
                if (parts.size < 5) {
                    null
                } else {
                    ManifestRow(
                        page = parts[0],
                        item = parts[1],
                        section = parts[2],
                        subsection = parts[3],
                        rarity = parts.drop(4).joinToString("\t"),
                    )
                }
            }
            .toList()

    /**
     * TOML `id` → wiki page: strip " Drops", underscore; longest-manifest-prefix fallback covers
     * split tables like "Dust devil Regular".
     */
    fun derivePageFromToml(tomlText: String, manifestPages: Set<String>): String {
        val id = TOML_ID.find(tomlText)?.groupValues?.get(1) ?: return ""
        val name = id.removeSuffix(" Drops")
        val candidate = name.replace(' ', '_')
        if (candidate in manifestPages) {
            return candidate
        }
        var best = ""
        for (page in manifestPages) {
            if (name.startsWith(page.replace('_', ' ')) && page.length > best.length) {
                best = page
            }
        }
        return best.ifEmpty { candidate }
    }

    fun derivePageFromKotlin(text: String): String {
        val identifier = KT_IDENTIFIER.find(text)?.groupValues?.get(1) ?: return ""
        return identifier.removeSuffix(" Drops").trim().replace(' ', '_')
    }

    fun scanTomlAnchors(monstersDir: Path, manifestPages: Set<String>): List<DropAnchor> =
        scan(monstersDir, "toml") { file, text ->
            val page = derivePageFromToml(text, manifestPages)
            text.lines().mapIndexedNotNull { index, line ->
                TOML_ANCHOR.matchEntire(line)?.let { match ->
                    DropAnchor(
                        file = file,
                        line = index + 1,
                        item = match.groupValues[1],
                        table = match.groupValues[2],
                        subsection = match.groupValues[3],
                        rarity = match.groupValues[4],
                        page = page,
                    )
                }
            }
        }

    fun scanKotlinAnchors(tablesDir: Path): List<DropAnchor> =
        scan(tablesDir, "kt") { file, text ->
            val page = derivePageFromKotlin(text)
            text.lines().mapIndexedNotNull { index, line ->
                KT_ANCHOR.matchEntire(line)?.let { match ->
                    DropAnchor(
                        file = file,
                        line = index + 1,
                        item = match.groupValues[1],
                        table = match.groupValues[2],
                        subsection = null,
                        rarity = match.groupValues[3],
                        page = page,
                    )
                }
            }
        }

    private fun scan(
        dir: Path,
        extension: String,
        parse: (Path, String) -> List<DropAnchor>,
    ): List<DropAnchor> =
        dir.listDirectoryEntries()
            .filter { it.extension == extension }
            .sortedBy { it.name }
            .flatMap { file -> parse(file, file.readText()) }
}
