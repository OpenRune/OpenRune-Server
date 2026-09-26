package org.rsmod.plugin.scan

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.util.concurrent.Executors

/**
 * A single classpath scan shared by every plugin-discovery consumer at boot (modules, scripts,
 * drop tables, ...), so the server doesn't pay for a separate full classpath walk over the same
 * `org.rsmod.api`/`org.rsmod.content` packages for each one.
 *
 * [scan] is computed lazily on first access and can be dropped with [release] once boot-time
 * discovery is done; the scan's class/annotation/field metadata for the whole plugin classpath is
 * worth tens of MB of live heap that nothing reads after startup. A later access simply rebuilds
 * it, so hot-loading an external plugin after release still works.
 */
public object PluginClasspathScan {
    public val searchPackages: Array<String> = arrayOf("org.rsmod.api", "org.rsmod.content")

    /** Resource path under which drop-table TOML definitions live, in addition to [searchPackages]. */
    public const val DROP_TABLE_RESOURCE_ROOT: String = "drops/tables"

    private val rejectPackages: Array<String> =
        arrayOf(
            "org.rsmod.api.*.integration",
            "org.rsmod.content.*.integration",
        )

    private val lock = Any()

    @Volatile private var cached: ScanResult? = null

    public val scan: ScanResult
        get() {
            cached?.let { return it }
            synchronized(lock) {
                cached?.let { return it }
                return performScan().also { cached = it }
            }
        }

    /**
     * Closes and forgets the current scan, if any. Safe to call more than once, and safe to call
     * while consumers still hold references to types they already resolved - only the scan's own
     * metadata index is freed.
     */
    public fun release(): Boolean {
        synchronized(lock) {
            val current = cached ?: return false
            cached = null
            current.close()
            return true
        }
    }

    private fun performScan(): ScanResult {
        val parallelism = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val pool = Executors.newWorkStealingPool(parallelism)
        try {
            return ClassGraph()
                .ignoreClassVisibility()
                .enableClassInfo()
                .enableFieldInfo()
                .enableAnnotationInfo()
                .disableNestedJarScanning()
                .disableModuleScanning()
                .rejectPackages(*rejectPackages)
                .acceptPackages(*searchPackages)
                .acceptPaths(DROP_TABLE_RESOURCE_ROOT)
                .scan(pool, parallelism)
        } finally {
            pool.shutdown()
        }
    }
}
