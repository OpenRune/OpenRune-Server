package org.rsmod.server.shared.loader

import com.github.michaelbull.logging.InlineLogger
import kotlin.time.Duration
import kotlin.time.measureTimedValue
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scan.PluginClasspathScan

object PluginModuleLoader {
    private val logger = InlineLogger()

    fun <T : PluginModule> load(type: Class<T>): Collection<T> {
        val modules = mutableListOf<T>()
        val timings = mutableListOf<Pair<String, Duration>>()
        val infoList = PluginClasspathScan.scan.getSubclasses(type).directOnly()
        for (info in infoList) {
            val (instance, duration) =
                measureTimedValue {
                    val clazz = info.loadClass(type)
                    val ctor = clazz.getConstructor()
                    ctor.newInstance()
                }
            timings += info.name to duration
            modules += instance
        }
        logger.info {
            val constructTotal = timings.fold(Duration.ZERO) { acc, (_, dur) -> acc + dur }
            val slowest =
                timings.sortedByDescending { it.second }.take(10).joinToString { (name, dur) ->
                    "$name=$dur"
                }
            "Loaded ${modules.size} plugin module(s); construction: $constructTotal " +
                "(classpath scan is shared via PluginClasspathScan); slowest to construct: $slowest"
        }
        return modules
    }
}
