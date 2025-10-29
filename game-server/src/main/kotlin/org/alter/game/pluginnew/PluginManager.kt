package org.alter.game.pluginnew

import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import java.io.File
import java.lang.reflect.Constructor

object PluginManager {

    private val logger = KotlinLogging.logger {}
    private val pkg = "org.alter"

    val scripts = ArrayList<Constructor<PluginEvent>>()

    @OptIn(DelicateCoroutinesApi::class)
    fun load() {
        val start = System.currentTimeMillis()
        val otherModuleClasses = File("../content/build/classes/kotlin/main")

        ClassGraph()
            .overrideClasspath(otherModuleClasses)
            .acceptPackages(pkg)
            .enableClassInfo()
            .scan().use { scanResult ->
                val pluginClassList = scanResult
                    .getSubclasses(PluginEvent::class.java.name)
                    .directOnly()

                pluginClassList.forEach { classInfo ->
                    try {
                        val clazz = classInfo.loadClass(PluginEvent::class.java)
                        val ctor = clazz.getDeclaredConstructor()
                        val instance = ctor.newInstance()

                        val initMethod = clazz.methods.find { it.name == "init" && it.parameterCount == 0 }
                        if (initMethod != null) {
                            initMethod.invoke(instance)
                        } else {
                            logger.warn { "Plugin ${clazz.name} has no init() method." }
                        }

                        scripts.add(ctor)
                    } catch (ex: Exception) {
                        logger.error(ex) { "Error loading plugin class: ${classInfo.name}" }
                    }
                }

                val totalTime = (System.currentTimeMillis() - start)
                logger.info { "Finished loading ${scripts.size} plugins in $totalTime ms." }
            }
    }
}