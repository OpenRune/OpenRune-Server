package dev.openrune.gamevals

import dev.openrune.definition.constants.GameValWrite
import dev.openrune.definition.constants.MutableMappingProvider
import dev.openrune.definition.constants.UnassignedGameVal
import dev.openrune.definition.constants.use
import dev.openrune.rscm.RSCMType
import java.io.File
import java.io.InputStream
import java.nio.file.Paths
import kotlin.io.use

class GameValProvider : MutableMappingProvider {

    override val mappings: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    val maxBaseID: MutableMap<String, Int> = mutableMapOf()

    private val sources: MutableMap<Pair<String, String>, File> = mutableMapOf()
    private val formats: MutableMap<File, SourceFormat> = mutableMapOf()
    private val unassigned: MutableList<UnassignedGameVal> = mutableListOf()

    private var generatedFile: File? = null

    companion object {
        const val UNASSIGNED_ID = -1

        fun sourceFiles(rootDir: String): Array<File> =
            arrayOf(
                Paths.get("${rootDir}.data", "gamevals-binary", "gamevals.dat").toFile(),
                Paths.get("${rootDir}.data", "gamevals-binary", GeneratedGameVals.FILE_NAME).toFile(),
                Paths.get("${rootDir}content").toFile(),
                Paths.get("${rootDir}api").toFile(),
                Paths.get("${rootDir}.data", "gamevals").toFile(),
            )

        fun load(rootDir: String = "") {
            GameValProvider().use(*sourceFiles(rootDir))
        }

        fun loadIsolated(rootDir: String = ""): GameValProvider {
            val provider = GameValProvider()
            provider.load(*sourceFiles(rootDir))
            return provider
        }
    }

    override fun load(vararg files: File) {
        require(files.isNotEmpty()) { "Expected at least gamevals.dat to load from" }

        sources.clear()
        formats.clear()
        unassigned.clear()

        decodeGameValDat(files[0])

        generatedFile = files.getOrNull(1)
        generatedFile?.takeIf { it.exists() }?.let { decodeGameValDat(it, updateMaxBaseId = false) }

        val contentDir = files.getOrNull(2)?.takeIf { it.exists() && it.isDirectory }
        val apiDir = files.getOrNull(3)?.takeIf { it.exists() && it.isDirectory }
        val gamevalsDir = files.getOrNull(4)?.takeIf { it.exists() && it.isDirectory }

        listOfNotNull(contentDir, apiDir).forEach { dir ->
            dir.walk()
                .filter {
                    it.isFile &&
                        it.name == "gamevals.toml" &&
                        !it.isGeneratedOutputPath()
                }
                .forEach(::processGameValToml)
        }

        gamevalsDir?.walk()
            ?.filter(File::isFile)
            ?.forEach(::processRSCMFile)

        collectUnassigned()
    }

    private fun collectUnassigned() {
        unassigned.clear()
        mappings.forEach { (table, entries) ->
            entries.forEach { (fullKey, value) ->
                if (value != UNASSIGNED_ID) return@forEach
                val key = fullKey.removePrefix("$table.")
                sources[table to key]?.let { unassigned += UnassignedGameVal(table, key, it) }
            }
        }
    }

    override fun unassignedGameVals(): List<UnassignedGameVal> = unassigned.toList()

    override fun maxBaseId(table: String): Int = maxBaseID[table] ?: -1

    override fun sourceOf(table: String, key: String): File? = sources[table to key]

    override fun writeGameVals(entries: List<GameValWrite>) {
        val (generated, declared) = entries.partition { it.generated }

        declared.groupBy { it.source }.forEach { (file, fileEntries) ->
            requireNotNull(file) {
                "No source file for declared gameval(s): ${fileEntries.joinToString { it.key }}"
            }
            writeFile(file, fileEntries)
        }

        if (generated.isNotEmpty()) {
            writeGeneratedDat(generated)
        }

        entries.forEach { entry ->
            mappings.getOrPut(entry.table) { mutableMapOf() }["${entry.table}.${entry.key}"] = entry.id
            if (!entry.generated) {
                entry.source?.let { sources[entry.table to entry.key] = it }
            }
        }

        unassigned.removeAll { placeholder ->
            declared.any { it.table == placeholder.table && it.key == placeholder.key }
        }
    }

    private fun writeGeneratedDat(entries: List<GameValWrite>) {
        val tables = entries
            .groupBy { it.table }
            .mapValues { (_, rows) -> rows.sortedBy { it.id }.map { "${it.key}=${it.id}" } }

        GeneratedGameVals.replaceTables(tables)
    }

    private fun writeFile(file: File, entries: List<GameValWrite>) {
        when (formats[file] ?: SourceFormat.of(file)) {
            SourceFormat.TOML -> writeTomlFile(file, entries)
            SourceFormat.RSCM -> writeRscmFile(file, entries)
        }
    }

    private fun writeTomlFile(file: File, entries: List<GameValWrite>) {
        val separator = file.lineSeparator()
        val lines = file.readLinesOrEmpty()

        fun indexOfKey(table: String, key: String): Int {
            var current: String? = null
            lines.forEachIndexed { index, line ->
                val trimmed = line.trim()
                GAMEVALS_SECTION_REGEX.find(trimmed)?.let {
                    current = it.groupValues[1]
                    return@forEachIndexed
                }
                if (current == table && parseGameValTomlEntry(trimmed, file.name)?.first == key) {
                    return index
                }
            }
            return -1
        }

        fun endOfSection(table: String): Int {
            val header = lines.indexOfFirst {
                GAMEVALS_SECTION_REGEX.find(it.trim())?.groupValues?.get(1) == table
            }
            if (header == -1) return -1
            var last = header
            for (index in header + 1 until lines.size) {
                if (GAMEVALS_SECTION_REGEX.matches(lines[index].trim())) break
                if (lines[index].isNotBlank()) last = index
            }
            return last
        }

        entries.filter { it.after == null }.forEach { entry ->
            val index = indexOfKey(entry.table, entry.key)
            require(index != -1) {
                "Cannot assign '${entry.key}' in ${file.name}: it is not declared under [gamevals.${entry.table}]"
            }
            lines[index] = "${entry.key} = ${entry.id}"
        }

        entries.filter { it.after != null }.forEach { entry ->
            val anchor = indexOfKey(entry.table, entry.after!!)
            val line = "${entry.key} = ${entry.id}"
            when {
                anchor != -1 -> lines.add(anchor + 1, line)
                endOfSection(entry.table) != -1 -> lines.add(endOfSection(entry.table) + 1, line)
                else -> {
                    if (lines.isNotEmpty() && lines.last().isNotBlank()) lines.add("")
                    lines.add("[gamevals.${entry.table}]")
                    lines.add(line)
                }
            }
        }

        file.parentFile?.mkdirs()
        file.writeText(lines.joinToString(separator, postfix = separator))
    }

    private fun writeRscmFile(file: File, entries: List<GameValWrite>) {
        val separator = file.lineSeparator()
        val lines = file.readLinesOrEmpty()

        fun indexOfKey(key: String): Int = lines.indexOfFirst { line ->
            line.isNotBlank() && runCatching { parseRSCMV2Line(line, 0).first }.getOrNull() == key
        }

        entries.filter { it.after == null }.forEach { entry ->
            val index = indexOfKey(entry.key)
            require(index != -1) {
                "Cannot assign '${entry.key}' in ${file.name}: the key is not declared in that file"
            }
            lines[index] = "${entry.key}=${entry.id}"
        }

        entries.filter { it.after != null }.forEach { entry ->
            val anchor = indexOfKey(entry.after!!)
            val line = "${entry.key}=${entry.id}"
            if (anchor == -1) lines.add(line) else lines.add(anchor + 1, line)
        }

        file.parentFile?.mkdirs()
        file.writeText(lines.joinToString(separator, postfix = separator))
    }

    private fun processGameValToml(file: File) {
        formats[file] = SourceFormat.TOML
        file.inputStream().use { stream -> processGameValToml(stream, file) }
    }

    private fun processGameValToml(input: InputStream, file: File) {
        var currentTable: String? = null

        input.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    return@forEach
                }

                GAMEVALS_SECTION_REGEX.find(trimmed)?.let { match ->
                    currentTable = match.groupValues[1].takeIf { it in RSCMType.RSCM_PREFIXES }
                    return@forEach
                }

                val table = currentTable ?: return@forEach
                val (key, value) = parseGameValTomlEntry(trimmed, file.name) ?: return@forEach

                mappings.putIfAbsent(table, mutableMapOf())
                val (parsedKey, parsedValue) = parseRSCMV2Line("$key=$value", 0)
                putMapping(table, parsedKey, parsedValue, file)
            }
        }
    }

    private fun parseGameValTomlEntry(line: String, source: String): Pair<String, Int>? {
        if (line.startsWith("[") || line.startsWith("#")) {
            return null
        }

        val equalsIndex = line.indexOf('=')
        if (equalsIndex <= 0) {
            return null
        }

        val key = line.substring(0, equalsIndex).trim()
        val value = line.substring(equalsIndex + 1).trim().toIntOrNull()
        if (value == null) {
            return null
        }

        if (key.isEmpty()) {
            throw IllegalArgumentException("Invalid empty key in $source: '$line'")
        }

        return key to value
    }

    private fun processRSCMFile(file: File) {
        val table = file.nameWithoutExtension
        val lines = file.readLines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return

        formats[file] = SourceFormat.RSCM
        mappings.putIfAbsent(table, mutableMapOf())

        lines.forEachIndexed { lineNumber, line ->
            try {
                val (key, value) = parseRSCMV2Line(line, lineNumber + 1)
                putMapping(table, key, value, file)
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to parse line ${lineNumber + 1} in ${file.name}: '$line'", e)
            }
        }
    }

    private fun putMapping(table: String, key: String, value: Int, file: File) {
        val tableMappings = mappings[table]
            ?: throw IllegalArgumentException("Table '$table' does not exist in mappings.")
        val fullKey = "$table.$key"

        fun remember() = run { sources[table to key] = file }

        if (value == UNASSIGNED_ID) {
            if (tableMappings[fullKey] != null && tableMappings[fullKey] != UNASSIGNED_ID) return
            tableMappings[fullKey] = UNASSIGNED_ID
            remember()
            return
        }

        val maxID = maxBaseID[table] ?: -1
        require(value > maxID) {
            "Custom value '$value' for key '$key' in table '$table' must exceed the current max base ID $maxID. " +
                "Cannot override existing osrs IDs."
        }

        val existingValueForKey = tableMappings[fullKey]
        if (existingValueForKey != null && existingValueForKey != UNASSIGNED_ID) {
            if (existingValueForKey == value) {
                remember()
                return
            }
            throw IllegalArgumentException(
                "Mapping conflict in table '$table': key '$fullKey' already exists with value " +
                    "'$existingValueForKey' (attempted '$value'). Keys must be unique."
            )
        }

        tableMappings.entries.find { it.value == value }?.let { existing ->
            throw IllegalArgumentException(
                "Mapping conflict in table '$table': value '$value' is already mapped to key " +
                    "'${existing.key}'. Values must be unique."
            )
        }

        tableMappings[fullKey] = value
        remember()
    }

    private fun parseRSCMV2Line(line: String, lineNumber: Int): Pair<String, Int> = when {
        line.contains("=") -> {
            val parts = line.split("=")
            require(parts.size == 2) { "Invalid line format at $lineNumber: '$line'. Expected 'key=value'" }
            parts[0].trim() to parts[1].trim().toInt()
        }
        line.contains(":") -> {
            val parts = line.split(":")
            require(parts.size == 2) { "Invalid sub-property format at $lineNumber: '$line'. Expected 'key:subprop=value'" }
            val key = parts[0].trim()
            val valueParts = parts[1].trim().split("=")
            require(valueParts.size == 2) { "Invalid sub-property value format at $lineNumber: '${parts[1]}'" }
            key to valueParts[1].trim().toInt()
        }
        else -> throw IllegalArgumentException(
            "Invalid line format at $lineNumber: '$line'. Expected 'key=value' or 'key:subprop=value'"
        )
    }

    private fun decodeGameValDat(datFile: File, updateMaxBaseId: Boolean = true) {
        GameValDat.read(datFile).forEach { (tableName, entries) ->
            mappings.putIfAbsent(tableName, mutableMapOf())

            entries.forEach { itemString ->
                try {
                    val (key, value) = parseRSCMV2Line(itemString, 0)
                    mappings[tableName]?.putIfAbsent("$tableName.$key", value)
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "Failed to parse item in table '$tableName' from ${datFile.name}: '$itemString'", e
                    )
                }
            }

            if (updateMaxBaseId) {
                maxBaseID[tableName] = mappings[tableName]?.values?.maxOrNull() ?: -1
            }
        }
    }

    override fun getSupportedExtensions(): List<String> = listOf(".rscm", ".rscm2")

    private enum class SourceFormat {
        TOML,
        RSCM;

        companion object {
            fun of(file: File): SourceFormat =
                if (file.name.endsWith(".toml")) TOML else RSCM
        }
    }
}

private fun File.isGeneratedOutputPath(): Boolean {
    val normalized = invariantSeparatorsPath
    return "/build/" in normalized || "/out/" in normalized || "/target/" in normalized
}

private fun File.lineSeparator(): String =
    if (exists() && readText().contains("\r\n")) "\r\n" else "\n"

private fun File.readLinesOrEmpty(): MutableList<String> =
    if (exists()) readLines().toMutableList() else mutableListOf()

private val GAMEVALS_SECTION_REGEX = Regex("^\\s*\\[gamevals\\.([^.\\]]+)\\]\\s*$")
