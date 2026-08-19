package dev.openrune.gamevals

import dev.openrune.definition.constants.MappingProvider
import dev.openrune.definition.constants.use
import dev.openrune.rscm.RSCMType
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Paths
import kotlin.io.use

class GameValProvider : MappingProvider {

    override val mappings: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    val maxBaseID: MutableMap<String, Int> = mutableMapOf()

    var autoAssignIds: Boolean = false

    companion object {
        fun sourceFiles(rootDir: String): Array<File> =
            arrayOf(
                Paths.get("${rootDir}.data", "gamevals-binary", "gamevals.dat").toFile(),
                Paths.get("${rootDir}.data", "gamevals-binary", "gamevals_columns.dat").toFile(),
                Paths.get("${rootDir}content").toFile(),
                Paths.get("${rootDir}api").toFile(),
                Paths.get("${rootDir}.data", "gamevals").toFile(),
            )

        fun load(rootDir: String = "", autoAssignIds: Boolean = false) {
            val provider = GameValProvider()
            provider.autoAssignIds = autoAssignIds
            provider.use(*sourceFiles(rootDir))
        }

        fun loadIsolated(rootDir: String = "", autoAssignIds: Boolean = false): GameValProvider {
            val provider = GameValProvider()
            provider.autoAssignIds = autoAssignIds
            provider.load(*sourceFiles(rootDir))
            return provider
        }
    }

    override fun load(vararg files: File) {
        require(files.size >= 2) {
            "Expected at least two files for loading: gamevals.dat and gamevals_columns.dat"
        }

        decodeGameValDat(files[0])
        decodeGameValDat(files[1])

        val contentDir = files.getOrNull(2)?.takeIf { it.exists() && it.isDirectory }
        val apiDir = files.getOrNull(3)?.takeIf { it.exists() && it.isDirectory }
        val gamevalsDir = files.getOrNull(4)?.takeIf { it.exists() && it.isDirectory }

        if (autoAssignIds && (contentDir != null || apiDir != null || gamevalsDir != null)) {
            GameValAutoAssigner(mappings, maxBaseID).run(contentDir, apiDir, gamevalsDir)
        }

        listOfNotNull(contentDir, apiDir).forEach { dir ->
            dir.walk()
                .filter { it.isFile && it.name == "gamevals.toml" && !it.isGeneratedOutputPath() }
                .forEach(::processGameValToml)
        }

        gamevalsDir?.walk()?.filter(File::isFile)?.forEach(::processRSCMFile)
    }

    private fun processGameValToml(file: File) {
        file.inputStream().use { stream -> processGameValToml(stream, file.name) }
    }

    private fun processGameValToml(input: InputStream, source: String) {
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
                val (key, value) = parseGameValTomlEntry(trimmed, source) ?: return@forEach

                mappings.putIfAbsent(table, mutableMapOf())
                val (parsedKey, parsedValue) = parseRSCMV2Line("$key=$value", 0)
                putMapping(table, parsedKey, parsedValue, source)
            }
        }
    }

    private fun parseGameValTomlEntry(line: String, source: String): Pair<String, Int>? {
        if (line.startsWith("[")) {
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

        mappings.putIfAbsent(table, mutableMapOf())

        lines.forEachIndexed { lineNumber, line ->
            try {
                val (key, value) = parseRSCMV2Line(line, lineNumber + 1)
                putMapping(table, key, value, file.name)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Failed to parse line ${lineNumber + 1} in ${file.name}: '$line'",
                    e,
                )
            }
        }
    }

    private fun putMapping(table: String, key: String, value: Int, file: String) {
        val tableMappings =
            mappings[table]
                ?: throw IllegalArgumentException("Table '$table' does not exist in mappings.")
        val fullKey = "$table.$key"

        val maxID = maxBaseID[table] ?: -1
        require(value > maxID) {
            "Custom value '$value' for key '$key' in table '$table' must exceed the current max base ID $maxID. " +
                "Cannot override existing osrs IDs."
        }

        val existingValueForKey = tableMappings[fullKey]
        if (existingValueForKey != null) {
            if (existingValueForKey == value) {
                return
            }
            throw IllegalArgumentException(
                "Mapping conflict in table '$table': key '$fullKey' already exists with value " +
                    "'$existingValueForKey' (attempted '$value'). Keys must be unique."
            )
        }

        tableMappings.entries
            .find { it.value == value }
            ?.let { existing ->
                throw IllegalArgumentException(
                    "Mapping conflict in table '$table': value '$value' is already mapped to key " +
                        "'${existing.key}'. Values must be unique."
                )
            }

        tableMappings[fullKey] = value
    }

    private fun parseRSCMV2Line(line: String, lineNumber: Int): Pair<String, Int> =
        when {
            line.contains("=") -> {
                val parts = line.split("=")
                require(parts.size == 2) {
                    "Invalid line format at $lineNumber: '$line'. Expected 'key=value'"
                }
                parts[0].trim() to parts[1].trim().toInt()
            }
            line.contains(":") -> {
                val parts = line.split(":")
                require(parts.size == 2) {
                    "Invalid sub-property format at $lineNumber: '$line'. Expected 'key:subprop=value'"
                }
                val key = parts[0].trim()
                val valueParts = parts[1].trim().split("=")
                require(valueParts.size == 2) {
                    "Invalid sub-property value format at $lineNumber: '${parts[1]}'"
                }
                key to valueParts[1].trim().toInt()
            }
            else ->
                throw IllegalArgumentException(
                    "Invalid line format at $lineNumber: '$line'. Expected 'key=value' or 'key:subprop=value'"
                )
        }

    private fun decodeGameValDat(datFile: File) {
        DataInputStream(FileInputStream(datFile)).use { input ->
            val tableCount = input.readInt()
            repeat(tableCount) {
                val nameLength = input.readShort().toInt()
                val nameBytes = ByteArray(nameLength)
                input.readFully(nameBytes)
                val tableName = String(nameBytes, Charsets.UTF_8)

                val itemCount = input.readInt()
                mappings.putIfAbsent(tableName, mutableMapOf())

                repeat(itemCount) {
                    val itemLength = input.readShort().toInt()
                    val itemBytes = ByteArray(itemLength)
                    input.readFully(itemBytes)
                    val itemString = String(itemBytes, Charsets.UTF_8)

                    try {
                        val (key, value) = parseRSCMV2Line(itemString, 0)
                        mappings[tableName]?.putIfAbsent("$tableName.$key", value)
                    } catch (e: Exception) {
                        throw IllegalArgumentException(
                            "Failed to parse item in table '$tableName' from ${datFile.name}: '$itemString'",
                            e,
                        )
                    }
                }

                maxBaseID[tableName] = mappings[tableName]?.values?.maxOrNull() ?: -1
            }
        }
    }

    override fun getSupportedExtensions(): List<String> = listOf(".rscm", ".rscm2")
}

private fun File.isGeneratedOutputPath(): Boolean {
    val normalized = invariantSeparatorsPath
    return "/build/" in normalized || "/out/" in normalized || "/target/" in normalized
}

private val GAMEVALS_SECTION_REGEX = Regex("^\\s*\\[gamevals\\.([^.\\]]+)\\]\\s*$")
