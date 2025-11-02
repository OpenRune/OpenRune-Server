package org.alter.game.pluginnew

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.system.exitProcess

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PluginConfig(val yamlFile: String)

open class PluginSettings {
    open val isEnabled: Boolean = true
}

object PluginManager {

    private val logger = KotlinLogging.logger {}
    const val PLUGIN_PACKAGE = "org.alter"
    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val yaml = Yaml()

    val scripts = mutableListOf<PluginEvent>()

    private val yamlCache = mutableMapOf<String, Map<String, Any>>()
    private val settingsCache = mutableMapOf<String, PluginSettings>()

    fun load() {
        val start = System.currentTimeMillis()
        val classOutputDir = File("../content/build/classes/kotlin/main")
        val resourcesDir = File("../content/build/resources/main/")

        ClassGraph()
            .overrideClasspath(classOutputDir)
            .acceptPackages(PLUGIN_PACKAGE)
            .enableClassInfo()
            .scan().use { scanResult ->

                val pluginClasses = scanResult
                    .getSubclasses(PluginEvent::class.java.name)
                    .directOnly()

                for (classInfo in pluginClasses) {
                    try {
                        val clazz = classInfo.loadClass(PluginEvent::class.java)
                        val instance = clazz.getDeclaredConstructor().newInstance()

                        loadPluginSettings(clazz, scanResult, resourcesDir)?.let { settings ->
                            instance.settings = settings
                        }

                        if (instance.isEnabled()) {
                            instance.init()
                            scripts.add(instance)
                        }

                    } catch (ex: Exception) {
                        logger.error(ex) { "Failed to load plugin: ${classInfo.name}" }
                        exitProcess(1)
                    }
                }
            }

        clear()
        val totalTime = System.currentTimeMillis() - start
        logger.info { "Finished loading ${scripts.size} plugins in ${totalTime}ms." }
    }

    private fun clear() {
        settingsCache.clear()
        yamlCache.clear()
    }

    private fun loadPluginSettings(
        clazz: Class<out PluginEvent>,
        scanResult: io.github.classgraph.ScanResult,
        resourcesDir: File
    ): PluginSettings? {

        val annotation = clazz.getAnnotation(PluginConfig::class.java) ?: return null
        val baseName = annotation.yamlFile.substringBeforeLast('.', annotation.yamlFile)

        val yamlName = findYamlFilename(baseName, resourcesDir) ?: run {
            logger.warn { "YAML file not found for plugin ${clazz.name}: $baseName(.yml/.yaml)" }
            return null
        }

        settingsCache[yamlName]?.let {
            return it
        }

        val yamlMap = yamlCache.getOrPut(yamlName) {
            val yamlFile = File(resourcesDir, yamlName)

            runCatching {
                yaml.load<Map<String, Any>>(yamlFile.readText())
            }.getOrElse {
                logger.error(it) { "Failed to parse YAML for ${clazz.name}" }
                return null
            }
        }

        val settingsClassName = "org.alter.settings.${baseName.replaceFirstChar { it.uppercase() }}Settings"
        val settingsClassInfo = scanResult.getClassInfo(settingsClassName)
        if (settingsClassInfo == null) {
            logger.warn { "Settings class not found: $settingsClassName" }
            return null
        }

        val settingsClazz = settingsClassInfo.loadClass()

        val settingsInstance = runCatching {
            objectMapper.convertValue(yamlMap, settingsClazz) as PluginSettings
        }.getOrElse {
            logger.error(it) { "Failed to map settings for ${clazz.name}" }
            return null
        }

        settingsCache[yamlName] = settingsInstance
        return settingsInstance
    }

    /**
     * Attempts to locate a YAML file supporting the following variations:
     *   - <name>
     *   - <name>.yml
     *   - <name>.yaml
     */
    private fun findYamlFilename(baseName: String, resourcesDir: File): String? {
        val candidates = listOf(
            File(resourcesDir, baseName),
            File(resourcesDir, "$baseName.yml"),
            File(resourcesDir, "$baseName.yaml")
        )
        return candidates.firstOrNull { it.exists() }?.name
    }
}
