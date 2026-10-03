package org.rsmod.plugin.loader

import com.github.michaelbull.logging.InlineLogger
import com.google.inject.AbstractModule
import com.google.inject.Injector
import dev.openrune.DirectoryConstants
import io.github.classgraph.ClassGraph
import java.io.File
import java.net.URLClassLoader
import java.util.Properties
import java.util.UUID
import java.util.jar.JarFile
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * A plugin source's `plugin.properties` (at the source's root — the top level of the jar or
 * directory), required for the source to be loadable at all. All four fields are mandatory.
 */
public data class PluginManifest(
    public val name: String,
    public val description: String,
    public val revision: String,
    public val author: String,
)

/** A known plugin source under [DirectoryConstants.PLUGINS_PATH] and its current state. */
public data class PluginStatus(
    public val id: String,
    public val manifest: PluginManifest?,
    public val enabled: Boolean,
    public val loaded: Boolean,
)

/**
 * Loads plugins from [DirectoryConstants.PLUGINS_PATH], on top of whatever is already on the
 * server's own classpath. A plugin source is either a `.jar` file or a plain directory of
 * compiled `.class` files — no packaging step is required, so a plugin under active development
 * can just point its build output at a folder in `plugins/`.
 *
 * Every source must carry a [PluginManifest] — a `plugin.properties` at its root with `name`,
 * `description`, `revision`, and `author` all set. A source without one, or with any of those four
 * fields blank, is refused everywhere ([loadModulesAtBoot], [loadScriptsAtBoot], [load]); it still
 * shows up in [listStatuses] (with a `null` manifest) so `::plugins` can point out what's missing.
 *
 * Sources present at boot participate the same way built-in plugins do: [loadModulesAtBoot] feeds
 * their [PluginModule]s into the same `Guice.createInjector(...)` call as the built-in ones, and
 * [loadScriptsAtBoot] constructs and starts their [PluginScript]s right after. Only sources marked
 * [PluginStatus.enabled] (see [setEnabled], persisted to `plugins/plugins-state.properties`)
 * participate at boot. Both passes reuse the same [ClassLoader] per source, so a module and a
 * script from the same plugin see the same class identity for any shared types.
 *
 * A source can be loaded on demand after boot with [load] (see the `::loadplugin`/`::plugins`
 * admin commands), without restarting the server. The main injector is already built by then, so
 * any [PluginModule]s a hot-loaded source declares only take effect in a child injector scoped to
 * that source's own [PluginScript]s — they can't introduce bindings visible anywhere else in the
 * running server. A hot-loaded [PluginScript] that only depends on services the server already
 * provides (the common case) works exactly like a built-in one.
 *
 * ## Reloading an already-loaded source
 *
 * Calling [load] on a source that's already loaded unloads it first: [PluginScript.shutdown] runs
 * on each of its script instances, then every `EventBus` (unbound/keyed/suspend) subscriber, every
 * `CheatCommandMap` command, and every `EngineQueueCache` "has a script" flag whose backing
 * lambda/method-reference class (or, for the queue cache, the classloader recorded at
 * registration) came from that source's classloader is removed. Jar sources are loaded through a
 * private runtime shadow copy, so the authoritative jar in `plugins/` stays replaceable on Windows
 * while the plugin is active. The source is then re-scanned and its scripts' `startup()` is re-run
 * against fresh instances.
 *
 * This still doesn't make a reload a full undo of everything the old code did:
 * - [PluginScript.shutdown] is opt-in. The engine has no way to know about or automatically
 *   reverse arbitrary side effects (entities spawned, shared state mutated, background coroutines
 *   started) — a script that cares about clean reloads needs to override `shutdown()` and clean
 *   those up itself. The default implementation does nothing.
 * - Effects from a `shutdown()` a script didn't override, or effects outside what it undoes, are
 *   not rolled back. A reload changes what runs for *future* dispatch; it can't retroactively
 *   fix state the old code already left behind.
 */
public object ExternalPluginLoader {
    private const val STATE_FILE_NAME = "plugins-state.properties"
    private const val MANIFEST_FILE_NAME = "plugin.properties"

    private val logger = InlineLogger()

    private val loadedSourcePaths = mutableSetOf<String>()
    private val loadedClassLoaders = mutableMapOf<String, ClassLoader>()
    private val loadedRuntimeCopies = mutableMapOf<String, File>()
    private val loadedScripts = mutableMapOf<String, List<PluginScript>>()
    private val disabledNames = mutableSetOf<String>()
    private var stateLoaded = false

    private val pluginsDir: File by lazy {
        DirectoryConstants.PLUGINS_PATH.toFile().apply { mkdirs() }
    }
    private val stateFile: File by lazy { File(pluginsDir, STATE_FILE_NAME) }
    private val runtimeDir: File by lazy {
        DirectoryConstants.DATA_PATH
            .resolve("plugin-runtime")
            .toFile()
            .apply {
                // A previous process may have left shadow jars behind. They are never authoritative;
                // the source under plugins/ is, so start every process with a clean runtime shadow.
                deleteRecursively()
                mkdirs()
            }
    }

    private fun ensureStateLoaded() {
        if (stateLoaded) return
        stateLoaded = true
        if (!stateFile.isFile) return
        val props = Properties()
        stateFile.inputStream().use(props::load)
        for (name in props.stringPropertyNames()) {
            if (props.getProperty(name).equals("false", ignoreCase = true)) {
                disabledNames += name
            }
        }
    }

    private fun saveState() {
        val props = Properties()
        for (source in listSources()) {
            val name = sourceName(source)
            props.setProperty(name, (name !in disabledNames).toString())
        }
        val comment = "External plugin enabled state - do not hand-edit while server is running"
        stateFile.outputStream().use { props.store(it, comment) }
    }

    private fun sourceName(file: File): String =
        if (file.isDirectory) file.name else file.nameWithoutExtension

    private fun isPluginSource(file: File): Boolean {
        val isJar = file.extension.equals("jar", ignoreCase = true)
        return file.name != STATE_FILE_NAME && (file.isDirectory || isJar)
    }

    private fun listSources(): List<File> =
        pluginsDir.listFiles(::isPluginSource)?.sortedBy { it.name }.orEmpty()

    private fun findSource(name: String): File? =
        listSources().find { source ->
            val matchesName = sourceName(source).equals(name, ignoreCase = true)
            val matchesFileName = source.name.equals(name, ignoreCase = true)
            matchesName || matchesFileName
        }

    private fun readManifestText(source: File): String? {
        if (source.isDirectory) {
            val file = File(source, MANIFEST_FILE_NAME)
            return if (file.isFile) file.readText() else null
        }
        return JarFile(source).use { jar ->
            val entry = jar.getEntry(MANIFEST_FILE_NAME) ?: return null
            jar.getInputStream(entry).bufferedReader().use { it.readText() }
        }
    }

    /**
     * Reads and validates [source]'s `plugin.properties`. Returns `null` if the file is missing,
     * unreadable, or has any of the four required fields blank.
     */
    private fun readManifest(source: File): PluginManifest? {
        val text = readManifestText(source) ?: return null
        val props = Properties()
        text.reader().use(props::load)
        val name = props.getProperty("name")?.trim().orEmpty()
        val description = props.getProperty("description")?.trim().orEmpty()
        val revision = props.getProperty("revision")?.trim().orEmpty()
        val author = props.getProperty("author")?.trim().orEmpty()
        if (name.isEmpty() || description.isEmpty() || revision.isEmpty() || author.isEmpty()) {
            return null
        }
        return PluginManifest(name, description, revision, author)
    }

    /** Every plugin source found in the plugins directory and its enabled/loaded state. */
    public fun listStatuses(): List<PluginStatus> {
        ensureStateLoaded()
        return listSources().map { source ->
            val name = sourceName(source)
            PluginStatus(
                id = name,
                manifest = readManifest(source),
                enabled = name !in disabledNames,
                loaded = source.canonicalPath in loadedSourcePaths,
            )
        }
    }

    /**
     * Marks [name] enabled/disabled and persists it. Disabling a source that's already loaded only
     * stops it from loading again on the next restart — it does not unload it (use [load] on it
     * again, which reloads, if you want to stop it now; see the class docs for what reload does
     * and doesn't undo).
     */
    public fun setEnabled(name: String, enabled: Boolean) {
        ensureStateLoaded()
        if (enabled) disabledNames -= name else disabledNames += name
        saveState()
    }

    /**
     * Enabled sources with a valid [PluginManifest], used by both boot passes. Logs a warning
     * (once per boot pass) for each enabled source that's missing one, so a plugin author notices
     * why their plugin never loaded.
     */
    private fun bootableSources(): List<File> {
        val enabled = listSources().filter { sourceName(it) !in disabledNames }
        return enabled.filter { source ->
            val hasManifest = readManifest(source) != null
            if (!hasManifest) {
                logger.warn {
                    "plugins/${sourceName(source)}: skipped, missing or invalid " +
                        "plugin.properties (needs name, description, revision, author)."
                }
            }
            hasManifest
        }
    }

    private fun classLoaderFor(source: File): ClassLoader =
        loadedClassLoaders.getOrPut(sourceName(source)) {
            newPluginClassLoader(source)
        }

    private fun newPluginClassLoader(source: File): URLClassLoader {
        val runtimeSource =
            if (source.isDirectory) {
                source
            } else {
                val id = sourceName(source)
                val shadow =
                    File(
                        runtimeDir,
                        "$id-${UUID.randomUUID()}.jar",
                    )
                source.copyTo(shadow, overwrite = false)
                loadedRuntimeCopies[id] = shadow
                shadow
            }
        return URLClassLoader(arrayOf(runtimeSource.toURI().toURL()), javaClass.classLoader)
    }

    /**
     * Kept only for source compatibility with callers from older builds.
     *
     * Active plugin classloaders must stay open until [unload]. Closing them after boot is unsafe:
     * event handlers, HTTP bridge callbacks, coroutines and other deferred work may legitimately
     * resolve plugin helper classes later and would fail with `NoClassDefFoundError`.
     */
    @Deprecated(
        message = "Active plugin classloaders must remain open; unload the plugin instead.",
        level = DeprecationLevel.WARNING,
    )
    public fun releaseAllClassLoaders(): Int = 0

    /** Discovers [PluginModule]s in every enabled, manifest-valid source at boot. */
    public fun loadModulesAtBoot(): List<AbstractModule> {
        ensureStateLoaded()
        val sources = bootableSources()
        val modules =
            sources.flatMap { source -> scanSourceModules(source, classLoaderFor(source)) }
        if (sources.isNotEmpty()) {
            val names = sources.joinToString { sourceName(it) }
            logger.info {
                "plugins/: found ${sources.size} source(s) [$names], contributing " +
                    "${modules.size} Guice module(s) to the injector."
            }
        }
        return modules
    }

    /** Discovers and constructs [PluginScript]s in every enabled, manifest-valid source at boot. */
    public fun loadScriptsAtBoot(injector: Injector): Collection<PluginScript> {
        ensureStateLoaded()
        val sources = bootableSources()
        val scripts =
            sources.flatMap { source ->
                scanSourceScripts(source, classLoaderFor(source), injector)
            }
        for (source in sources) {
            loadedSourcePaths += source.canonicalPath
        }
        if (sources.isNotEmpty()) {
            val names = sources.joinToString { sourceName(it) }
            logger.info {
                "plugins/: started ${scripts.size} script(s) from source(s) [$names]."
            }
        }
        return scripts
    }

    /**
     * Loads and starts the plugin source named [name] (a directory, or a `.jar` file, matched with
     * or without its extension). If the source is already loaded, it's unloaded first (see the
     * class docs for exactly what that does and doesn't undo) and then loaded fresh — this is
     * what makes `::pluginreload` an actual reload rather than a refusal.
     *
     * Returns `null` (and logs a warning) if no such source exists, if it's disabled, or if it has
     * no valid `plugin.properties`.
     */
    public fun load(
        name: String,
        injector: Injector,
        scriptContext: ScriptContext,
    ): List<PluginScript>? {
        ensureStateLoaded()
        val source = findSource(name)
        if (source == null) {
            logger.warn { "plugins/$name: not found (no matching file or folder)." }
            return null
        }
        if (sourceName(source) in disabledNames) {
            logger.warn { "plugins/${sourceName(source)}: disabled, run ::pluginenable first." }
            return null
        }
        val manifest = readManifest(source)
        if (manifest == null) {
            logger.warn {
                "plugins/${sourceName(source)}: missing or invalid plugin.properties " +
                    "(needs name, description, revision, author)."
            }
            return null
        }

        val wasLoaded = source.canonicalPath in loadedSourcePaths
        if (wasLoaded) {
            unload(source, scriptContext)
        }

        val loader = newPluginClassLoader(source)
        val modules = scanSourceModules(source, loader)
        val effectiveInjector =
            if (modules.isEmpty()) injector else injector.createChildInjector(modules)
        val scripts = scanSourceScripts(source, loader, effectiveInjector)
        for (script in scripts) {
            with(script) { scriptContext.startup() }
        }

        loadedSourcePaths += source.canonicalPath
        loadedClassLoaders[sourceName(source)] = loader
        loadedScripts[sourceName(source)] = scripts
        logger.info {
            val verb = if (wasLoaded) "reloaded" else "loaded"
            "plugins/${sourceName(source)}: $verb '${manifest.name}' rev ${manifest.revision} " +
                "by ${manifest.author} - ${modules.size} module(s), ${scripts.size} script(s) " +
                "started."
        }
        return scripts
    }

    /**
     * Runs shutdown hooks and unregisters everything the plugin source named [name] registered,
     * without loading anything in its place. Returns `false` (no-op) if no such source is
     * currently loaded. See the class docs for what this does and doesn't cover.
     */
    public fun unload(name: String, scriptContext: ScriptContext): Boolean {
        val source = findSource(name) ?: return false
        if (source.canonicalPath !in loadedSourcePaths) return false
        unload(source, scriptContext)
        return true
    }

    /**
     * Runs [PluginScript.shutdown] on [source]'s scripts, then unregisters every `EventBus`
     * subscriber, `CheatCommandMap` command, and `EngineQueueCache` binding that [source]'s
     * classloader defined, and closes that classloader. See the class docs for what this does and
     * doesn't cover.
     *
     * Jar plugins run from a private shadow copy, so the authoritative source jar in `plugins/`
     * stays replaceable while the plugin is active. Unload closes the classloader and deletes that
     * runtime shadow before a reload creates the next one.
     */
    private fun unload(source: File, scriptContext: ScriptContext) {
        val id = sourceName(source)
        val loader = loadedClassLoaders.remove(id) ?: return
        val scripts = loadedScripts.remove(id).orEmpty()
        for (script in scripts) {
            with(script) { scriptContext.shutdown() }
        }
        val events = scriptContext.eventBus.removeByClassLoader(loader)
        val commands = scriptContext.cheatCommandMap.removeByClassLoader(loader)
        val queues = scriptContext.engineQueueCache.removeByClassLoader(loader)
        (loader as? URLClassLoader)?.close()
        loadedRuntimeCopies.remove(id)?.delete()
        loadedSourcePaths -= source.canonicalPath
        logger.info {
            "plugins/$id: unloaded - ran ${scripts.size} shutdown hook(s), removed $events " +
                "event handler(s), $commands command(s), $queues engine-queue binding(s)."
        }
    }

    private fun scanSourceModules(source: File, loader: ClassLoader): List<AbstractModule> =
        ClassGraph()
            .ignoreClassVisibility()
            .enableClassInfo()
            .overrideClasspath(source)
            .overrideClassLoaders(loader)
            .scan()
            .use { scan ->
                scan
                    .getSubclasses(PluginModule::class.java)
                    .directOnly()
                    .map { info -> info.loadClass(PluginModule::class.java) }
                    .map { clazz -> clazz.getConstructor().newInstance() }
            }

    private fun scanSourceScripts(
        source: File,
        loader: ClassLoader,
        injector: Injector,
    ): List<PluginScript> =
        ClassGraph()
            .ignoreClassVisibility()
            .enableClassInfo()
            .overrideClasspath(source)
            .overrideClassLoaders(loader)
            .scan()
            .use { scan ->
                scan
                    .getSubclasses(PluginScript::class.java)
                    .filter { info -> !info.isAbstract && !info.isInterface }
                    .map { info -> info.loadClass(PluginScript::class.java) }
                    .map { clazz -> injector.getInstance(clazz) }
            }
}
