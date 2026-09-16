package org.rsmod.server.shared.loader

import com.github.michaelbull.logging.InlineLogger
import com.google.inject.Injector
import jakarta.inject.Inject
import java.lang.reflect.Modifier
import kotlin.time.Duration
import kotlin.time.measureTimedValue
import org.rsmod.plugin.scan.PluginClasspathScan
import org.rsmod.plugin.scripts.PluginScript

class PluginScriptLoader @Inject constructor() {
    private val logger = InlineLogger()

    fun <T : PluginScript> load(
        type: Class<T>,
        injector: Injector,
        lenient: Boolean = false,
    ): Collection<T> {
        val (classes, lookupDuration) =
            measureTimedValue {
                PluginClasspathScan.scan
                    .getSubclasses(type)
                    .parallelStream()
                    .map { info -> info.loadClass(type) }
                    .filter { clazz ->
                        !Modifier.isAbstract(clazz.modifiers) && !Modifier.isInterface(clazz.modifiers)
                    }
                    .toList()
            }

        val plugins = ArrayList<T>(classes.size)
        val timings = mutableListOf<Pair<String, Duration>>()
        for (clazz in classes) {
            try {
                val (instance, duration) = measureTimedValue { injector.getInstance(clazz) }
                timings += clazz.name to duration
                plugins += instance
            } catch (t: Throwable) {
                if (!lenient) throw (t.cause ?: t)
                t.printStackTrace()
            }
        }

        logger.info {
            val constructTotal = timings.fold(Duration.ZERO) { acc, (_, dur) -> acc + dur }
            val slowest =
                timings.sortedByDescending { it.second }.take(10).joinToString { (name, dur) ->
                    "$name=$dur"
                }
            "Loaded ${plugins.size} plugin script(s); subclass lookup (shared scan): " +
                "$lookupDuration, construction: $constructTotal; slowest to construct: $slowest"
        }

        return plugins
    }
}
