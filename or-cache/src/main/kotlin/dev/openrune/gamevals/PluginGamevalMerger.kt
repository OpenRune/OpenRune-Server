package dev.openrune.gamevals

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import dev.openrune.rscm.RSCMType
import java.io.File
import java.io.InputStream

object PluginGamevalMerger {
    private val tomlMapper = ObjectMapper(TomlFactory()).findAndRegisterModules()

    fun merge(rootDir: File) {
        val gamevalsDir = File(rootDir, ".data/gamevals").apply { mkdirs() }
        val contentDir = File(rootDir, "content")
        if (!contentDir.isDirectory) {
            return
        }

        contentDir
            .walk()
            .filter { it.isFile && it.name == "gamevals.toml" && !it.isGeneratedGamevalPath() }
            .forEach { mergeTomlFile(it, gamevalsDir) }
    }

    fun mergeTomlStream(input: InputStream, source: String, gamevalsDir: File) {
        val root: Map<String, Any?> =
            tomlMapper.readValue(input, object : TypeReference<Map<String, Any?>>() {})
        mergeTomlRoot(root, source, gamevalsDir)
    }

    private fun mergeTomlFile(file: File, gamevalsDir: File) {
        file.inputStream().use { mergeTomlStream(it, file.name, gamevalsDir) }
    }

    private fun mergeTomlRoot(root: Map<String, Any?>, source: String, gamevalsDir: File) {
        val gamevalsSection = root["gamevals"] as? Map<*, *> ?: return

        gamevalsSection.forEach { (tableNameAny, tableValuesAny) ->
            val tableName = tableNameAny as? String ?: return@forEach
            val tableValues = tableValuesAny as? Map<*, *> ?: return@forEach

            require(tableName in RSCMType.RSCM_PREFIXES) {
                "Invalid TOML table '$tableName' in $source. Expected one of: ${RSCMType.RSCM_PREFIXES}"
            }

            val rscmFile = File(gamevalsDir, "$tableName.rscm")
            val existingKeys =
                if (rscmFile.exists()) {
                    rscmFile
                        .readLines()
                        .filter { it.isNotBlank() && !it.startsWith("#") }
                        .mapNotNull { line -> line.substringBefore("=").trim().takeIf { it.isNotEmpty() } }
                        .toSet()
                } else {
                    emptySet()
                }

            val appended =
                buildString {
                    tableValues.forEach { (k, v) ->
                        val key = k.toString()
                        if (key in existingKeys) {
                            return@forEach
                        }

                        val value =
                            when (v) {
                                is Number -> v.toInt()
                                is String -> v.toIntOrNull() ?: return@forEach
                                else -> return@forEach
                            }

                        appendLine("$key=$value")
                    }
                }

            if (appended.isNotEmpty()) {
                if (rscmFile.exists() && rscmFile.length() > 0L) {
                    val endsWithNewline =
                        rscmFile
                            .readBytes()
                            .lastOrNull()
                            ?.toInt()
                            ?.toChar() == '\n'
                    if (!endsWithNewline) {
                        rscmFile.appendText("\n")
                    }
                }
                rscmFile.appendText(appended)
            }
        }
    }
}

fun main(args: Array<String>) {
    val root = args.firstOrNull()?.let(::File) ?: File(System.getProperty("user.dir"))
    PluginGamevalMerger.merge(root)
}

private fun File.isGeneratedGamevalPath(): Boolean {
    val normalized = invariantSeparatorsPath
    return "/build/" in normalized || "/out/" in normalized || "/target/" in normalized
}
