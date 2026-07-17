package org.rsmod.server.shared.loader

import com.google.inject.Injector
import io.github.classgraph.ClassGraph
import jakarta.inject.Inject
import java.lang.reflect.Modifier
import java.util.concurrent.Executors
import org.rsmod.annotations.PluginGraph
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.server.shared.util.use

class PluginScriptLoader @Inject constructor(@PluginGraph private val scanner: ClassGraph) {
    fun <T : PluginScript> load(
        type: Class<T>,
        injector: Injector,
        lenient: Boolean = false,
    ): Collection<T> {
        val parallelism = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val classes =
            Executors.newWorkStealingPool(parallelism).use { pool ->
                scanner.scan(pool, parallelism).use { result ->
                    result
                        .getSubclasses(type)
                        .parallelStream()
                        .map { info -> info.loadClass(type) }
                        .filter { clazz ->
                            !Modifier.isAbstract(clazz.modifiers) &&
                                !Modifier.isInterface(clazz.modifiers)
                        }
                        .toList()
                }
            }

        val plugins = ArrayList<T>(classes.size)
        for (clazz in classes) {
            try {
                plugins += injector.getInstance(clazz)
            } catch (t: Throwable) {
                if (!lenient) throw (t.cause ?: t)
                t.printStackTrace()
            }
        }
        return plugins
    }
}
