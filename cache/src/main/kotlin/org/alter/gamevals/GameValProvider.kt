package org.alter.gamevals

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import dev.openrune.definition.constants.MappingProvider
import dev.openrune.definition.constants.use
import org.alter.rscm.RSCMType
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream
import java.nio.file.Paths
import kotlin.io.use

class GameValProvider : MappingProvider {

    private val tomlMapper = ObjectMapper(TomlFactory()).findAndRegisterModules()
    override val mappings: MutableMap<String, MutableMap<String, Int>> = mutableMapOf()
    val maxBaseID: MutableMap<String, Int> = mutableMapOf()

    companion object {
        fun load(rootDir: String = "../") {
            val provider = GameValProvider()
            provider.use(
                Paths.get("${rootDir}data", "gamevals.dat").toFile(),
                Paths.get("${rootDir}data", "gamevals_columns.dat").toFile(),
                Paths.get("${rootDir}content", "src", "main", "resources", "org", "alter").toFile(),
                Paths.get("${rootDir}data", "cfg", "gamevals-custom").toFile()
            )
        }
    }

    override fun load(vararg files: File) {
        require(files.size >= 2) { "Expected at least two files for loading: gamevals.dat and gamevals_columns.dat" }

        decodeGameValDat(files[0])
        decodeGameValDat(files[1])

        // Load TOML
        files.getOrNull(2)?.takeIf { it.exists() }?.walk()
            ?.filter { it.isFile && it.name == "gamevals.toml" }
            ?.forEach { processGameValToml(it) }

        // Load RSCM directories
        files.drop(3).forEach { dir ->
            require(dir.isDirectory) { "Expected a directory for RSCM mappings, got file: ${dir.absolutePath}" }
            dir.walk().filter { it.isFile }.forEach { processRSCMFile(it) }
        }
    }

    private fun processGameValToml(file: File) {
        val root: Map<String, Any?> = tomlMapper.readValue(file, object : TypeReference<Map<String, Any?>>() {})
        val gamevalsSection = root["gamevals"] as? Map<*, *> ?: return

        gamevalsSection.forEach { (tableNameAny, tableValuesAny) ->
            val tableName = tableNameAny as? String ?: return@forEach
            val tableValues = tableValuesAny as? Map<*, *> ?: return@forEach

            require(tableName in RSCMType.RSCM_PREFIXES) {
                "Invalid TOML table '$tableName' in ${file.name}. Expected one of: ${RSCMType.RSCM_PREFIXES}"
            }

            mappings.putIfAbsent(tableName, mutableMapOf())

            tableValues.forEach { (k, v) ->
                val key = k.toString()
                val value = when (v) {
                    is Number -> v.toInt()
                    is String -> v.toIntOrNull() ?: return@forEach
                    else -> return@forEach
                }

                val (parsedKey, parsedValue) = parseRSCMV2Line("$key=$value", 0)
                putMapping(tableName, parsedKey, parsedValue, file.name)
            }
        }
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
                throw IllegalArgumentException("Failed to parse line ${lineNumber + 1} in ${file.name}: '$line'", e)
            }
        }
    }

    private fun putMapping(table: String, key: String, value: Int, file: String) {
        val tableMappings = mappings[table]
            ?: throw IllegalArgumentException("Table '$table' does not exist in mappings.")

        val maxID = maxBaseID[table] ?: -1
        require(value > maxID) {
            "Custom value '$value' for key '$key' in table '$table must exceed the current max base ID $maxID. " +
                    "Cannot override existing osrs IDs."
        }

        if (tableMappings.containsKey(key)) {
            throw IllegalArgumentException(
                "Mapping conflict in table '$table: key '$key' already exists. Keys must be unique."
            )
        }

        tableMappings.entries.find { it.value == value }?.let { existing ->
            throw IllegalArgumentException(
                "Mapping conflict in table '$table: value '$value' is already mapped to key '${existing.key}'. Values must be unique."
            )
        }

        tableMappings["$table.$key"] = value
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
                            "Failed to parse item in table '$tableName' from ${datFile.name}: '$itemString'", e
                        )
                    }
                }

                maxBaseID[tableName] = mappings[tableName]?.values?.maxOrNull() ?: -1
            }
        }
    }

    override fun getSupportedExtensions(): List<String> = listOf(".rscm", ".rscm2")
}