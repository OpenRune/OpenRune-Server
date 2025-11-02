package org.alter.plugin.settings

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import org.alter.game.pluginnew.PluginManager.PLUGIN_PACKAGE
import org.alter.game.pluginnew.PluginSettings
import org.alter.rscm.RSCMType
import java.io.File

class PluginSettingsSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val logger = environment.logger
    private val generatedFiles = mutableSetOf<String>()
    private val rscmPrefixes = RSCMType.entries.map { it.prefix }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleDir = environment.options["moduleDir"]
            ?: throw IllegalArgumentException("moduleDir KSP option not provided!")

        val resourcesDir = File(moduleDir, "src/main/resources/")
        if (!resourcesDir.exists()) {
            logger.warn("Resources directory does not exist: $resourcesDir")
            return emptyList()
        }

        resourcesDir.walkTopDown()
            .filter { it.isFile && it.extension.lowercase() in listOf("yml", "yaml", "toml") }
            .forEach { file ->
                val baseName = file.nameWithoutExtension.replaceFirstChar { it.uppercase() } + "Settings"
                if (generatedFiles.add(baseName)) {
                    val mapper = when (file.extension.lowercase()) {
                        "yml", "yaml" -> ObjectMapper(YAMLFactory())
                        "toml" -> ObjectMapper(TomlFactory())
                        else -> return@forEach
                    }.registerModule(KotlinModule())

                    val data: Map<String, Any> =
                        mapper.readValue(file, object : TypeReference<Map<String, Any>>() {})
                    if (data.isNotEmpty()) generateClasses(data, baseName, file.name)
                }
            }

        return emptyList()
    }

    private fun generateClasses(data: Any, baseName: String, fileName: String) {
        val pkg = "$PLUGIN_PACKAGE.settings"
        val fileSpec = FileSpec.builder(pkg, baseName)
            .addImport("org.alter.game.pluginnew", "PluginSettings")
            .addImport("com.fasterxml.jackson.annotation", "JsonProperty")
            .addAnnotation(
                AnnotationSpec.builder(Suppress::class)
                    .addMember("%S", "RedundantVisibilityModifier")
                    .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
                    .build()
            )
            .addType(buildClass(pascalCase(baseName), data, root = true))
            .build()

        fileSpec.writeTo(environment.codeGenerator, aggregating = false)
        logger.info("Generated PluginSettings for $fileName")
    }

    private fun buildClass(className: String, value: Any?, root: Boolean = false): TypeSpec {
        val fields = (value as? Map<*, *>)?.mapNotNull { (k, v) ->
            (k as? String)?.let { camelCase(it) to v }
        }?.toMap() ?: emptyMap()

        val classBuilder = TypeSpec.classBuilder(className)
            .addModifiers(KModifier.DATA) // Make it a data class
        if (root) classBuilder.superclass(PluginSettings::class)

        // Primary constructor
        val constructor = FunSpec.constructorBuilder().apply {
            if (root) {
                addParameter(
                    ParameterSpec.builder("isEnabled", BOOLEAN)
                        .defaultValue("true")
                        .build()
                )
            }

            fields.forEach { (name, v) ->
                val type = inferType(name, v)
                val defaultValue = defaultValueForField(v)
                val paramType = if (defaultValue == CodeBlock.of("null")) type.copy(nullable = true) else type

                addParameter(
                    ParameterSpec.builder(name, paramType)
                        .apply { defaultValue?.let { defaultValue(it) } }
                        .build()
                )
            }
        }.build()
        classBuilder.primaryConstructor(constructor)

        // Properties
        fields.forEach { (name, v) ->
            val type = inferType(name, v)
            val prop = PropertySpec.builder(name, type)
                .initializer(name)
                .apply {
                    if (originalKeyFromValue(name) != name) {
                        addAnnotation(
                            AnnotationSpec.builder(JsonProperty::class)
                                .addMember("%S", originalKeyFromValue(name))
                                .useSiteTarget(AnnotationSpec.UseSiteTarget.FIELD)
                                .build()
                        )
                    }
                }
                .build()
            classBuilder.addProperty(prop)
        }

        if (root) {
            classBuilder.addProperty(
                PropertySpec.builder("isEnabled", BOOLEAN)
                    .initializer("isEnabled")
                    .addModifiers(KModifier.OVERRIDE)
                    .build()
            )
        }

        // Nested classes for maps/lists only
        fields.forEach { (name, v) ->
            when (v) {
                is Map<*, *> -> if (v.values.any { it is Map<*, *> || it is List<*> }) {
                    classBuilder.addType(buildClass(pascalCase(name), v, root = false))
                }
                is List<*> -> {
                    val first = v.firstOrNull()
                    if (first is Map<*, *>) {
                        classBuilder.addType(buildClass(pascalCase(name.removeSuffix("s")), first, root = false))
                    }
                }
            }
        }

        return classBuilder.build()
    }

    private fun defaultValueForField(v: Any?): CodeBlock? = when {
        v is Map<*, *> && v.values.any { it is Map<*, *> || it is List<*> } -> CodeBlock.of("null") // nested class
        v is Map<*, *> -> CodeBlock.of("emptyMap()") // primitive map
        v is List<*> && v.firstOrNull() is Map<*, *> -> CodeBlock.of("emptyList()") // list of objects
        v is List<*> -> CodeBlock.of("emptyList()") // list of primitives
        else -> null // primitives have no default
    }

    private fun inferType(key: String, value: Any?): TypeName = when (value) {
        is Int -> INT
        is Double -> DOUBLE
        is Float -> FLOAT
        is Boolean -> BOOLEAN
        is String -> if (rscmPrefixes.any { value.startsWith("$it.") }) INT else STRING
        is List<*> -> {
            val first = value.firstOrNull()
            if (first is Map<*, *>) LIST.parameterizedBy(ClassName("", pascalCase(key.removeSuffix("s"))))
            else LIST.parameterizedBy(inferType(key, first))
        }
        is Map<*, *> -> {
            val allPrimitive = value.all { it.key is String && it.value !is Map<*, *> && it.value !is List<*> }
            if (allPrimitive) MAP.parameterizedBy(STRING, value.values.firstOrNull()?.let { inferType(key, it) } ?: ANY)
            else ClassName("", pascalCase(key)).copy(nullable = true)
        }
        else -> ANY.copy(nullable = true)
    }

    private fun originalKeyFromValue(camelCaseName: String) =
        camelCaseName.fold(StringBuilder()) { sb, c ->
            if (c.isUpperCase()) sb.append('_').append(c.lowercaseChar()) else sb.append(c)
        }.toString()

    private fun camelCase(input: String): String {
        val parts = input.split('_')
        return parts.first() + parts.drop(1).joinToString("") { it.replaceFirstChar(Char::uppercase) }
    }

    private fun pascalCase(input: String): String =
        input.split('_').joinToString("") { it.replaceFirstChar(Char::uppercase) }
}
