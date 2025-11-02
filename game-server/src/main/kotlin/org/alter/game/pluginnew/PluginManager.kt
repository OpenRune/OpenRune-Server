package org.alter.game.pluginnew

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import org.yaml.snakeyaml.Yaml
import java.io.File
import kotlin.system.exitProcess

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PluginSetting(val yamlFile: String)

object PluginManager {

    private val logger = KotlinLogging.logger {}
    public val pkg = "org.alter"

    val scripts = mutableListOf<PluginEvent>()

    private val objectMapper = ObjectMapper().findAndRegisterModules()
    private val yaml = Yaml()

    /** Load all plugins and assign their settings */
    fun load() {
        val start = System.currentTimeMillis()

        val otherModuleClasses = File("../content/build/classes/kotlin/main")
        val settingsFolder = File("content/build/resources/main/")

        ClassGraph()
            .overrideClasspath(otherModuleClasses)
            .acceptPackages(pkg)
            .enableClassInfo()
            .scan().use { scanResult ->

                val pluginClasses = scanResult
                    .getSubclasses(PluginEvent::class.java.name)
                    .directOnly()

                pluginClasses.forEach { classInfo ->
                    try {
                        val clazz = classInfo.loadClass(PluginEvent::class.java)
                        val ctor = clazz.getDeclaredConstructor()
                        val instance = ctor.newInstance()

                        // --- Handle @PluginSetting ---
                        val annotation = clazz.getAnnotation(PluginSetting::class.java)
                        if (annotation != null) {
                            val yamlFile = File("C:\\Users\\Home\\Desktop\\Alter-feature-support-231\\Alter-feature-support-231\\content\\build\\resources\\main\\", annotation.yamlFile)
                            if (yamlFile.exists()) {
                                val map = yaml.load<Map<String, Any>>(yamlFile.readText())

                                val baseName = annotation.yamlFile.substringBeforeLast(".")
                                val settingsClassName = baseName.replaceFirstChar { it.uppercase() } + "Settings"
                                val settingsClassInfo = scanResult.getClassInfo("org.alter.settings.$settingsClassName")

                                if (settingsClassInfo != null) {
                                    val settingsClazz = settingsClassInfo.loadClass() as Class<out Any>
                                    val settingsInstance = objectMapper.convertValue(map, settingsClazz)
                                    instance.settings = settingsInstance
                                    logger.info { "Loaded settings for plugin ${clazz.name} from ${annotation.yamlFile}" }
                                } else {
                                    logger.warn { "Settings class not found: org.alter.settings.$settingsClassName" }
                                }

                                logger.info { "Loaded settings for plugin ${clazz.name} from ${annotation.yamlFile}" }
                            }
                        }

                        // --- Call init() ---
                        val initMethod = clazz.methods.find { it.name == "init" && it.parameterCount == 0 }
                        initMethod?.invoke(instance)

                        scripts.add(instance)
                        logger.info { "Loaded plugin: ${clazz.name}" }

                    } catch (ex: Exception) {
                        logger.error(ex) { "Failed to load plugin: ${classInfo.name}" }
                        ex.printStackTrace()
                        exitProcess(1)
                    }
                }

                val totalTime = System.currentTimeMillis() - start
                logger.info { "Finished loading ${scripts.size} plugins in $totalTime ms." }
            }
    }
}