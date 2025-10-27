package org.alter.game.pluginnew

import io.github.classgraph.ClassGraph
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.reflect.Constructor
import java.net.URLClassLoader

object PluginManager {

    val logger = KotlinLogging.logger {}

    private val pkg = "org.alter"

    val scripts = ArrayList<Constructor<Script>>()

    @OptIn(DelicateCoroutinesApi::class)
    fun load() {
        val otherModuleClasses = File("../content/build/classes/kotlin/main")

        ClassGraph()
            .overrideClasspath(otherModuleClasses)
            .acceptPackages(pkg)
            .enableClassInfo()
            .scan().use { scanResult ->
                val pluginClassList = scanResult.getSubclasses(Script::class.java.name).directOnly()

                pluginClassList.forEach { classInfo ->
                    try {
                        val clazz = classInfo.loadClass(Script::class.java)
                        val instance = clazz.getDeclaredConstructor().newInstance()
                        scripts.add(clazz.getDeclaredConstructor())
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                logger.info { "Finished loading plugins. Total loaded: ${scripts.size}" }
            }
    }
}