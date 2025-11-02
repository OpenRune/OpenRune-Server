package org.alter.plugin.settings

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import org.alter.game.pluginnew.PluginManager.PLUGIN_PACKAGE
import org.alter.game.pluginnew.PluginSettings
import org.yaml.snakeyaml.Yaml
import java.io.File

class PluginSettingsSymbolProcessor(
    private val environment: SymbolProcessorEnvironment
) : SymbolProcessor {

    private val generatedFiles = mutableSetOf<String>()
    private val logger = environment.logger

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val moduleDir = environment.options["moduleDir"]
            ?: throw IllegalArgumentException("moduleDir KSP option not provided!")

        val resourcesDir = File(moduleDir, "src/main/resources/")
        if (!resourcesDir.exists()) {
            logger.warn("Resources directory does not exist: $resourcesDir")
            return emptyList()
        }

        resourcesDir.walkTopDown()
            .filter { it.isFile && (it.extension in listOf("yml", "yaml")) }
            .forEach { file ->
                val baseName = "${file.nameWithoutExtension.replaceFirstChar { it.uppercase() }}Settings"
                if (generatedFiles.add(baseName)) {
                    generateFromYaml(file, baseName)
                }
            }

        return emptyList()
    }

    private fun generateFromYaml(file: File, baseName: String) {
        val yaml = Yaml()
        val data = yaml.load<Any>(file.readText()) ?: return

        val pkg = "${PLUGIN_PACKAGE}.settings"
        val fileSpecBuilder = FileSpec.builder(pkg, baseName)

        val generatedClasses = mutableListOf<TypeSpec>()
        val rootClass = buildDataClass(baseName, data, generatedClasses)

        fileSpecBuilder.addType(rootClass)
        generatedClasses.forEach { fileSpecBuilder.addType(it) }

        // Write to KSP's generated code output
        val codeGenerator = environment.codeGenerator
        fileSpecBuilder.build().writeTo(codeGenerator, aggregating = false)

        logger.info("Generated data class for ${file.name}")
    }

    private fun buildDataClass(
        className: String,
        value: Any?,
        nestedClasses: MutableList<TypeSpec>
    ): TypeSpec {
        val fields: Map<String, TypeName> = (value as? Map<*, *>)?.mapNotNull { entry ->
            val k = entry.key
            val v = entry.value
            if (k !is String) return@mapNotNull null
            val typeName = inferType(k, v, nestedClasses)
            k to typeName
        }?.toMap() ?: emptyMap()

        val classBuilder = TypeSpec.classBuilder(className)
            .superclass(PluginSettings::class)
            .primaryConstructor(
                FunSpec.constructorBuilder().apply {
                    fields.forEach { (name, type) ->
                        addParameter(name, type)
                    }
                }.build()
            )

        fields.forEach { (name, type) ->
            val propBuilder = PropertySpec.builder(name, type)
                .initializer(name)

            if (name == "isEnabled") {
                propBuilder.addModifiers(KModifier.OVERRIDE)
            }

            classBuilder.addProperty(propBuilder.build())
        }

        return classBuilder.build()
    }

    private fun inferType(
        key: String,
        value: Any?,
        nestedClasses: MutableList<TypeSpec>
    ): TypeName = when (value) {
        is Int -> INT
        is Double -> DOUBLE
        is Float -> FLOAT
        is Boolean -> BOOLEAN
        is String -> STRING
        is List<*> -> {
            val first = value.firstOrNull()
            if (first is Map<*, *>) {
                val className = key.replaceFirstChar { it.uppercase() }
                val nested = buildDataClass(className, first, nestedClasses)
                nestedClasses += nested
                LIST.parameterizedBy(ClassName("", className))
            } else {
                LIST.parameterizedBy(inferType(key, first, nestedClasses))
            }
        }
        is Map<*, *> -> {
            val className = key.replaceFirstChar { it.uppercase() }
            val nested = buildDataClass(className, value, nestedClasses)
            nestedClasses += nested
            ClassName("", className)
        }
        else -> ANY.copy(nullable = true)
    }
}