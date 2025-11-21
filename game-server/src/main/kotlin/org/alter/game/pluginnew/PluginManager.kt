package org.alter.game.pluginnew

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.toml.TomlFactory
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import org.alter.game.model.World
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.lang.reflect.Modifier
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
    private val tomlMapper = ObjectMapper(TomlFactory()).findAndRegisterModules()
    private val yaml = Yaml()

    val scripts = mutableListOf<PluginEvent>()

    private val yamlCache = mutableMapOf<String, Map<String, Any>>()
    private val settingsCache = mutableMapOf<String, PluginSettings>()

    fun load(world: World) {
        val start = System.currentTimeMillis()
        val classOutputDir = File("../content/build/classes/kotlin/main")
        val resourcesDir = File("../content/build/resources/main/")

        ClassGraph()
            .overrideClasspath(classOutputDir)
            .acceptPackages(PLUGIN_PACKAGE)
            .enableClassInfo()
            .scan().use { scanResult ->

                // Get all subclasses of PluginEvent (including indirect)
                val pluginClasses = scanResult
                    .getSubclasses(PluginEvent::class.java.name)

                pluginClasses
                    .mapNotNull { classInfo ->
                        try {
                            val clazz = classInfo.loadClass()

                            // Only concrete classes that are assignable to PluginEvent
                            if (!PluginEvent::class.java.isAssignableFrom(clazz)) return@mapNotNull null
                            if (Modifier.isAbstract(clazz.modifiers)) return@mapNotNull null

                            @Suppress("UNCHECKED_CAST")
                            clazz as Class<out PluginEvent>
                        } catch (ex: Exception) {
                            logger.error(ex) { "Failed to load class: ${classInfo.name}" }
                            null
                        }
                    }
                    .forEach { pluginClass ->
                        try {
                            val instance = pluginClass.getDeclaredConstructor().newInstance()

                            // Load plugin settings if available
                            loadPluginSettings(pluginClass, scanResult, resourcesDir)?.let { settings ->
                                instance.settings = settings
                            }

                            instance.world = world

                            if (instance.isEnabled()) {
                                instance.init()
                                scripts.add(instance)
                            }
                        } catch (ex: Exception) {
                            logger.error(ex) { "Failed to instantiate plugin: ${pluginClass.name}" }
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

        val configFileName = findConfigFilename(baseName, resourcesDir) ?: run {
            logger.warn { "Config file not found for plugin ${clazz.name}: $baseName(.yml/.yaml/.toml)" }
            return null
        }

        settingsCache[configFileName]?.let { return it }

        val configMap = yamlCache.getOrPut(configFileName) {
            val file = File(resourcesDir, configFileName)

            val fileText = postProcessSettings(file.readText())

            runCatching {
                when (file.extension.lowercase()) {
                    "yml", "yaml" -> yaml.load(fileText)
                    "toml" -> tomlMapper.readValue(fileText, Map::class.java) as Map<String, Any>
                    else -> emptyMap()
                }
            }.getOrElse {
                logger.error(it) { "Failed to parse config for ${clazz.name}" }
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
            objectMapper.convertValue(configMap, settingsClazz) as PluginSettings
        }.getOrElse {
            logger.error(it) { "Failed to map settings for ${clazz.name}" }
            return null
        }

        settingsCache[configFileName] = settingsInstance
        return settingsInstance
    }

    fun postProcessSettings(content: String): String {
        val prefixes = RSCMType.entries.map { it.prefix }
        if (prefixes.isEmpty()) return content

        val combinedRegex = Regex("""["']?\b(${prefixes.joinToString("|")})\.[\w.]+["']?""")

        return combinedRegex.replace(content) { matchResult ->
            val raw = matchResult.value.trim('"', '\'')
            RSCM.getRSCM(raw).toString()
        }
    }

    /**
     * Attempts to locate a config file supporting the following variations:
     *   - <name>
     *   - <name>.yml
     *   - <name>.yaml
     *   - <name>.toml
     */
    private fun findConfigFilename(baseName: String, resourcesDir: File): String? {
        val candidates = listOf(
            File(resourcesDir, baseName),
            File(resourcesDir, "$baseName.yml"),
            File(resourcesDir, "$baseName.yaml"),
            File(resourcesDir, "$baseName.toml")
        )
        return candidates.firstOrNull { it.exists() }?.name
    }
}
