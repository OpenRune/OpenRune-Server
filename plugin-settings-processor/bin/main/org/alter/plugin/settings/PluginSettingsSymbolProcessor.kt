package org.alter.plugin.settings

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import org.alter.game.pluginnew.PluginManager.PACKAGE
import org.yaml.snakeyaml.Yaml
import java.io.File

class PluginSettingsSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val generatedFiles = mutableSetOf<String>()

    private val codeGenerator = environment.codeGenerator
    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleDir = environment.options["moduleDir"] ?: throw IllegalArgumentException("moduleDir KSP option not provided!")

        val resourcesDir = File(moduleDir, "src/main/resources/")

        if (!resourcesDir.exists()) {
            logger.warn("Resources directory does not exist: $resourcesDir")
        } else {
            resourcesDir.walkTopDown()
                .filter { it.isFile && (it.extension == "yml" || it.extension == "yaml") }
                .forEach { file ->
                    val baseName = "${file.nameWithoutExtension.replaceFirstChar { it.uppercase() }}Settings"
                    if (baseName !in generatedFiles) {
                        generateFromYaml(file)
                        generatedFiles += baseName
                    }
                }
        }

        return emptyList()
    }

    private fun generateFromYaml(yamlFile: File) {
        val yaml = Yaml()
        val data = yaml.load<Any>(yamlFile.readText()) ?: return
        val pkg = "${PACKAGE}.settings"
        val baseName = "${yamlFile.nameWithoutExtension.replaceFirstChar { it.uppercase() }}Settings"
        val outputFile = codeGenerator.createNewFile(Dependencies(false), pkg, baseName)

        val builder = StringBuilder("package $pkg\n\n")

        val classes = mutableListOf<Pair<String, Map<String, Any?>>>()

        // Recursively inspect data
        val rootFields = extractFields(data, classes, baseName)

        builder.append(generateDataClass(baseName, rootFields))
        for ((name, fields) in classes) {
            builder.append("\n\n").append(generateDataClass(name, fields))
        }

        outputFile.bufferedWriter().use { it.write(builder.toString()) }

        logger.info("Generated data class for ${yamlFile.name}")
    }

    /** Extract fields and recursively find nested structures */
    private fun extractFields(value: Any?, classes: MutableList<Pair<String, Map<String, Any?>>>, className: String): Map<String, Any?> {
        return when (value) {
            is Map<*, *> -> {
                value.mapNotNull { (k, v) ->
                    if (k !is String) return@mapNotNull null
                    val typeName = inferType(k, v, classes)
                    k to typeName
                }.toMap()
            }
            else -> emptyMap()
        }
    }

    /** Infer Kotlin type name from YAML value */
    private fun inferType(key: String, value: Any?, classes: MutableList<Pair<String, Map<String, Any?>>>): String {
        return when (value) {
            is Int -> "Int"
            is Double -> "Double"
            is Float -> "Float"
            is Boolean -> "Boolean"
            is String -> "String"
            is List<*> -> {
                val first = value.firstOrNull()
                if (first is Map<*, *>) {
                    val subName = key.replaceFirstChar { it.uppercase() }
                    val subFields = extractFields(first, classes, subName)
                    classes += subName to subFields
                    "List<$subName>"
                } else {
                    val type = inferType(key, first, classes)
                    "List<$type>"
                }
            }
            is Map<*, *> -> {
                val subName = key.replaceFirstChar { it.uppercase() }
                val subFields = extractFields(value, classes, subName)
                classes += subName to subFields
                subName
            }
            else -> "Any?"
        }
    }

    private fun generateDataClass(name: String, fields: Map<String, Any?>): String {
        val content = fields.entries.joinToString(",\n    ") {
            "val ${it.key}: ${it.value}"
        }
        return "data class $name(\n    $content\n)"
    }
}
