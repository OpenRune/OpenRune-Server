package dev.openrune.tools

import dev.openrune.gamevals.GameValProvider
import dev.openrune.rscm.RSCMType
import java.io.File

/**
 * Collects gameval references in Kotlin, TOML and TSV files and resolves each
 * one against the gamevals the server loads at startup, so a symbol that no table defines shows up
 * here instead of as a runtime "Missing mapping" error.
 */
public object GamevalChecker {
    public data class Reference(val key: String, val file: String, val line: Int)

    public data class Result(
        val scannedFiles: Int,
        val references: List<Reference>,
        val missing: Map<String, List<Reference>>,
        val unassigned: Map<String, List<Reference>>,
    )

    private val SKIPPED_DIRS =
        setOf(
            "build", "out", ".gradle", ".git", ".idea", ".kotlin", ".claude", "node_modules",
            "docs", "openwiki", "generated", "playground",
        )

    private val NAME_CHARS = Regex("[A-Za-z0-9_:\\-]+")
    private val TSV_REFERENCE = Regex("(?<![\\w.])[a-z]+\\.[A-Za-z0-9_:\\-]+")

    public fun scan(root: File): Pair<Int, List<Reference>> {
        val references = ArrayList<Reference>()
        var scanned = 0

        root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS && !(it.name == "test" && it.parentFile?.name == "src") }
            .filter { it.isFile }
            .forEach { file ->
                val collector: (String, Int) -> Unit = { literal, line ->
                    val key = parseReference(literal)
                    if (key != null) {
                        references += Reference(key, file.relativeTo(root).invariantSeparatorsPath, line)
                    }
                }
                when {
                    file.extension == "kt" || file.extension == "kts" -> {
                        scanned++
                        KotlinStringScanner(file.readText(), collector).run()
                    }
                    file.extension == "toml" && file.name != "gamevals.toml" -> {
                        scanned++
                        scanToml(file, collector)
                    }
                    file.extension == "tsv" -> {
                        scanned++
                        file.useLines { lines ->
                            lines.forEachIndexed { index, line ->
                                if (!line.trimStart().startsWith("#")) {
                                    TSV_REFERENCE.findAll(line).forEach { collector(it.value, index + 1) }
                                }
                            }
                        }
                    }
                }
            }
        return scanned to references
    }

    public fun check(
        root: File,
        provider: GameValProvider,
        reportFile: File? = null,
        generated: Set<String> = emptySet(),
    ): Result {
        val (scanned, references) = scan(root)
        val missing = LinkedHashMap<String, MutableList<Reference>>()
        val unassigned = LinkedHashMap<String, MutableList<Reference>>()

        for (reference in references) {
            val table = reference.key.substringBefore('.')
            val entries = provider.mappings[table]
            when (val id = entries?.get(reference.key)) {
                null ->
                    if (
                        reference.key !in generated &&
                        entries?.keys?.any { it.startsWith("${reference.key}:") } != true
                    ) {
                        missing.getOrPut(reference.key) { ArrayList() } += reference
                    }
                GameValProvider.UNASSIGNED_ID ->
                    unassigned.getOrPut(reference.key) { ArrayList() } += reference
            }
        }

        val result =
            Result(
                scannedFiles = scanned,
                references = references,
                missing = missing.toSortedMap(),
                unassigned = unassigned.toSortedMap(),
            )
        report(result, reportFile)
        return result
    }

    private fun report(result: Result, reportFile: File?) {
        val distinct = result.references.map { it.key }.toSet()
        println(
            "Scanned ${result.scannedFiles} files: ${result.references.size} gameval references, " +
                "${distinct.size} distinct"
        )
        distinct.groupBy { it.substringBefore('.') }.toSortedMap().forEach { (table, keys) ->
            val absent = keys.count { it in result.missing }
            println("  %-14s %6d distinct, %d missing".format(table, keys.size, absent))
        }
        printSection("Missing", "defined in no gamevals table", result.missing, limit = 3)
        printSection("Unassigned", "declared but no id assigned yet", result.unassigned, limit = 3)
        if (result.missing.isEmpty() && result.unassigned.isEmpty()) {
            println("All gameval references resolve.")
        }

        if (reportFile != null) {
            val text =
                buildString {
                    appendSection("Missing", result.missing, limit = Int.MAX_VALUE)
                    appendSection("Unassigned", result.unassigned, limit = Int.MAX_VALUE)
                }
            reportFile.parentFile?.mkdirs()
            reportFile.writeText(text.ifEmpty { "All gameval references resolve.\n" })
            println("Full report: ${reportFile.canonicalPath}")
        }
    }

    private fun printSection(
        title: String,
        why: String,
        entries: Map<String, List<Reference>>,
        limit: Int,
    ) {
        if (entries.isEmpty()) return
        println()
        println("$title ($why): ${entries.size} gamevals")
        print(buildString { appendEntries(entries, limit) })
    }

    private fun StringBuilder.appendSection(
        title: String,
        entries: Map<String, List<Reference>>,
        limit: Int,
    ) {
        if (entries.isEmpty()) return
        appendLine("$title: ${entries.size} gamevals")
        appendEntries(entries, limit)
        appendLine()
    }

    private fun StringBuilder.appendEntries(entries: Map<String, List<Reference>>, limit: Int) {
        entries.entries.groupBy { it.key.substringBefore('.') }.toSortedMap().forEach { (table, group) ->
            appendLine("  [$table] ${group.size}")
            group.forEach { (key, uses) ->
                appendLine("    $key (${uses.size} use${if (uses.size == 1) "" else "s"})")
                uses.take(limit).forEach { appendLine("      ${it.file}:${it.line}") }
                if (uses.size > limit) appendLine("      +${uses.size - limit} more")
            }
        }
    }

    private fun parseReference(literal: String): String? {
        val dot = literal.indexOf('.')
        if (dot <= 0 || dot == literal.lastIndex) return null
        if (literal.substring(0, dot) !in RSCMType.RSCM_PREFIXES) return null
        val name = literal.substring(dot + 1)
        return literal.takeIf { NAME_CHARS.matches(name) && !name.endsWith('_') }
    }

    private fun scanToml(file: File, onLiteral: (String, Int) -> Unit) {
        val quoted = Regex("\"([^\"\\\\]*)\"|'([^']*)'")
        file.readLines().forEachIndexed { index, line ->
            if (line.trimStart().startsWith("#")) return@forEachIndexed
            quoted.findAll(line).forEach { match ->
                onLiteral(match.groupValues[1].ifEmpty { match.groupValues[2] }, index + 1)
            }
        }
    }

    /**
     * Walks Kotlin source and reports the content of plain string literals. Comments, char literals
     * and strings with templates are skipped; `${ }` expressions are lexed as code so a nested
     * literal keeps the surrounding string in sync.
     */
    private class KotlinStringScanner(
        private val text: String,
        private val onLiteral: (String, Int) -> Unit,
    ) {
        private var pos = 0
        private var line = 1

        fun run() = code(nested = false)

        private fun peek(offset: Int): Char = text.getOrElse(pos + offset) { '\u0000' }

        private fun code(nested: Boolean) {
            var depth = 0
            while (pos < text.length) {
                when (text[pos]) {
                    '\n' -> {
                        line++
                        pos++
                    }
                    '/' ->
                        when (peek(1)) {
                            '/' -> skipLineComment()
                            '*' -> skipBlockComment()
                            else -> pos++
                        }
                    '"' -> string()
                    '\'' -> charLiteral()
                    '{' -> {
                        depth++
                        pos++
                    }
                    '}' -> {
                        pos++
                        if (nested && depth == 0) return
                        depth--
                    }
                    else -> pos++
                }
            }
        }

        private fun skipLineComment() {
            while (pos < text.length && text[pos] != '\n') pos++
        }

        private fun skipBlockComment() {
            var depth = 0
            while (pos < text.length) {
                when {
                    text[pos] == '/' && peek(1) == '*' -> {
                        depth++
                        pos += 2
                    }
                    text[pos] == '*' && peek(1) == '/' -> {
                        depth--
                        pos += 2
                        if (depth == 0) return
                    }
                    else -> {
                        if (text[pos] == '\n') line++
                        pos++
                    }
                }
            }
        }

        private fun charLiteral() {
            pos++
            while (pos < text.length && text[pos] != '\'' && text[pos] != '\n') {
                if (text[pos] == '\\') pos++
                pos++
            }
            pos++
        }

        private fun string() {
            if (peek(1) == '"' && peek(2) == '"') {
                rawString()
                return
            }

            val startLine = line
            val value = StringBuilder()
            var dynamic = false
            pos++
            while (pos < text.length) {
                val c = text[pos]
                when {
                    c == '"' -> {
                        pos++
                        if (!dynamic) onLiteral(value.toString(), startLine)
                        return
                    }
                    c == '\n' -> return
                    c == '\\' -> {
                        value.append(peek(1))
                        pos += 2
                    }
                    c == '$' && peek(1) == '{' -> {
                        dynamic = true
                        pos += 2
                        code(nested = true)
                    }
                    c == '$' && (peek(1).isLetter() || peek(1) == '_') -> {
                        dynamic = true
                        pos++
                    }
                    else -> {
                        value.append(c)
                        pos++
                    }
                }
            }
        }

        private fun rawString() {
            pos += 3
            while (pos < text.length) {
                when {
                    text[pos] == '"' && peek(1) == '"' && peek(2) == '"' -> {
                        pos += 3
                        while (pos < text.length && text[pos] == '"') pos++
                        return
                    }
                    text[pos] == '$' && peek(1) == '{' -> {
                        pos += 2
                        code(nested = true)
                    }
                    else -> {
                        if (text[pos] == '\n') line++
                        pos++
                    }
                }
            }
        }
    }
}
