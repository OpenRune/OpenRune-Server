package org.rsmod.server.app

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.michaelbull.logging.InlineLogger
import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.util.Modules
import dev.openrune.DirectoryConstants
import dev.openrune.ServerCacheManager
import dev.openrune.filesystem.Cache
import dev.openrune.map.GameMapDecoder
import dev.openrune.map.GameMapSpawnSink
import dev.openrune.map.npc.MapNpcDefinition
import dev.openrune.map.obj.MapObjDefinition
import java.nio.file.Path
import java.text.DecimalFormat
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.time.Duration
import kotlin.time.TimeSource
import kotlin.time.measureTimedValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.rsmod.api.game.process.PluginScriptBootGate
import org.rsmod.api.repo.npc.NpcRepository
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.entity.Npc
import org.rsmod.game.map.LocZoneStorage
import org.rsmod.game.obj.Obj
import org.rsmod.game.obj.ObjEntity
import org.rsmod.game.obj.ObjScope
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.module.PluginModule
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.server.install.GameNetworkRsaGenerator
import org.rsmod.server.install.GameServerLogbackCopy
import org.rsmod.server.shared.PluginConstants
import org.rsmod.server.shared.loader.PluginModuleLoader
import org.rsmod.server.shared.loader.PluginScriptLoader

fun main(args: Array<String>): Unit = GameServer().main(args)

class GameServer(private val skipTypeVerificationOverride: Boolean? = null) :
    CliktCommand(name = "server") {
    private val logger = InlineLogger()

    private val pluginPackages: Array<String>
        get() = PluginConstants.searchPackages

    private val vanillaCacheDir: Path
        get() = DirectoryConstants.CACHE_PATH.resolve("LIVE")

    private val gameConfig: Path
        get() = Path("game.yml")

    private val gameCacheDir: Path
        get() = DirectoryConstants.CACHE_PATH.resolve("SERVER")

    private val rsaKey: Path
        get() = DirectoryConstants.DATA_PATH.resolve("game.key")

    private val skipTypeVerificationOption: Boolean by
        option(
                "--skip-type-verification",
                help = "Skip identity hash verification for cache type resolver.",
            )
            .flag(default = false)

    private lateinit var serverConfig: ServerConfig

    // When the app is run in integration tests, the GameServer is constructed directly and Clikt
    // args are not parsed. In that case, we fall back to the explicit override to avoid accessing
    // the uninitialized `skipTypeVerificationOption` delegate.
    private val skipTypeVerification: Boolean
        get() = skipTypeVerificationOverride ?: skipTypeVerificationOption

    override fun run() {
        val bootMark = TimeSource.Monotonic.markNow()
        ensureProperInstallation()
        startApplication(bootMark)
    }

    private fun startApplication(bootMark: TimeSource.Monotonic.ValueTimeMark) {
        val phases = LinkedHashMap<String, Duration>()
        try {
            val pluginModules = timedPhase(phases, "plugin-modules") { loadModules() }
            val injector =
                timedPhase(phases, "guice") {
                    Guice.createInjector(
                        Modules.combine(GameServerModule, *pluginModules.toTypedArray()),
                    )
                }
            prepareGame(injector, phases)
            startupGame(injector, phases, bootMark)
        } catch (_: ServerRestartException) {}
    }

    fun createInjector(): Injector {
        val pluginModules = loadModules()
        val modules = Modules.combine(GameServerModule, *pluginModules.toTypedArray())
        return Guice.createInjector(modules)
    }

    fun prepareGame(injector: Injector) {
        prepareGame(injector, phases = null)
    }

    private fun prepareGame(injector: Injector, phases: MutableMap<String, Duration>?) {
        serverConfig = timedPhase(phases, "config") { loadConfig(injector) }
        val or2cache = timedPhase(phases, "cache") { ServerCacheManager.init(serverConfig.revision) }
        timedPhase(phases, "map") { loadMap(or2cache, injector) }
        if (phases == null) {
            loadScripts(injector)
        }
    }

    private fun loadModules(): Collection<AbstractModule> {
        logger.info { "Loading plugin modules..." }
        return PluginModuleLoader.load(PluginModule::class.java, pluginPackages)
    }

    private fun loadMap(or2cache: Cache, injector: Injector) {
        logger.info { "Loading game map and collision flags..." }
        val npcRepo = injector.getInstance(NpcRepository::class.java)
        val objRepo = injector.getInstance(ObjRepository::class.java)

        val sink =
            object : GameMapSpawnSink {
                override fun onNpcSpawn(def: MapNpcDefinition, coords: CoordGrid) {
                    val type =
                        ServerCacheManager.getNpc(def.id)
                            ?: error("Invalid npc type: $def ($coords)")
                    val npc = Npc(type, coords)
                    npcRepo.addDelayed(npc, spawnDelay = 0, duration = Int.MAX_VALUE)
                }

                override fun onObjSpawn(def: MapObjDefinition, coords: CoordGrid) {
                    val type =
                        ServerCacheManager.getItem(def.id)
                            ?: error("Invalid obj type: $def ($coords)")
                    val entity =
                        ObjEntity(type.id, count = def.count, scope = ObjScope.Perm.id)
                    val obj =
                        Obj(
                            coords,
                            entity,
                            creationCycle = 0,
                            receiverId = Obj.NULL_OBSERVER_ID,
                        )
                    objRepo.addDelayed(obj, spawnDelay = 0, duration = Int.MAX_VALUE)
                }
            }

        GameMapDecoder.decodeAll(sink, or2cache)

        val locZoneStorage = injector.getInstance(LocZoneStorage::class.java)
        logger.info {
            "Loaded ${DecimalFormat().format(locZoneStorage.mapZoneCount())} static zones and " +
                "${DecimalFormat().format(locZoneStorage.mapLocCount())} locs."
        }
    }

    private fun loadConfig(injector: Injector): ServerConfig {
        logger.info { "Loading server config..." }
        val config = injector.getInstance(ServerConfig::class.java)
        logger.info { "Loaded server config: $config" }
        return config
    }

    private fun loadScripts(injector: Injector) {
        logger.info { "Loading plugin scripts..." }
        val scriptLoader = injector.getInstance(PluginScriptLoader::class.java)
        val scripts = scriptLoader.load(PluginScript::class.java, injector)
        val scriptContext = injector.getInstance(ScriptContext::class.java)
        for (script in scripts) {
            startupPluginScript(script, scriptContext)
        }
        injector.getInstance(PluginScriptBootGate::class.java).markReady()
        logger.info { "Loaded ${scripts.size} script${if (scripts.size == 1) "" else "s"}." }
    }

    private fun startupGame(
        injector: Injector,
        phases: MutableMap<String, Duration>,
        bootMark: TimeSource.Monotonic.ValueTimeMark,
    ) {
        logger.info { "Loading server bootstrap..." }
        lateinit var bootstrap: GameBootstrap
        timedPhase(phases, "bootstrap-get") {
            bootstrap = injector.getInstance(GameBootstrap::class.java)
        }

        lateinit var shutdownHook: Thread
        runBlocking {
            val scriptsDeferred = async(Dispatchers.Default) {
                timedPhase(phases, "scripts") { loadScripts(injector) }
            }
            timedPhase(phases, "services-start") {
                shutdownHook = bootstrap.startupUntilReady()
            }
            scriptsDeferred.await()
        }

        val total = bootMark.elapsedNow()
        val breakdown = phases.entries.joinToString { (name, duration) -> "$name=$duration" }
        logger.info { "Server ready in $total ($breakdown)" }

        bootstrap.awaitShutdown(shutdownHook)
    }

    private fun startupPluginScript(script: PluginScript, context: ScriptContext) {
        with(script) { context.startup() }
    }

    private inline fun <T> timedPhase(
        phases: MutableMap<String, Duration>?,
        name: String,
        block: () -> T,
    ): T {
        val (result, duration) = measureTimedValue(block)
        phases?.put(name, duration)
        return result
    }

    /**
     * Checks if all configurations required to run the server properly are in place. If not, the
     * appropriate installation tasks are run before resuming the normal game app boot-up.
     */
    private fun ensureProperInstallation() {
        val gameCacheDirExists = gameCacheDir.isDirectory()
        val vanillaCacheDirExists = vanillaCacheDir.isDirectory()
        val validRsaKey = rsaKey.isRegularFile()

        if (!vanillaCacheDirExists || !gameCacheDirExists || !gameConfig.exists()) {
            error("Please run the install task first: gradlew install")
        }

        if (!validRsaKey) {
            GameServerLogbackCopy().main(emptyArray())
            GameNetworkRsaGenerator().main(emptyArray())
            return
        }
    }

    /**
     * Thrown to immediately abort the current server startup process when a cache update requires a
     * server restart.
     *
     * After performing the necessary cache update and calling `startApplication` to restart the
     * server, this exception is thrown to ensure that no further initialization occurs in the
     * current execution context. It is caught at the top level and safely ignored.
     */
    private class ServerRestartException : Exception()
}
