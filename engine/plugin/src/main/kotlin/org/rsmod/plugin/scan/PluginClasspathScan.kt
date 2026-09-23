package org.rsmod.plugin.scan

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.util.concurrent.Executors

/**
 * A single classpath scan shared by every plugin-discovery consumer at boot (modules, scripts,
 * drop tables, ...), so the server doesn't pay for a separate full classpath walk over the same
 * `org.rsmod.api`/`org.rsmod.content` packages for each one.
 *
 * [scan] is computed once, lazily, on first access from any consumer, and is intentionally never
 * closed: plugin discovery only happens during boot, and keeping the scan's in-memory metadata
 * alive for the rest of the process is far cheaper than re-scanning or worrying about a second
 * consumer touching it after another closed it.
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

    public val scan: ScanResult by lazy { performScan() }

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
