package org.rsmod.server.app

import jakarta.inject.Inject
import java.util.concurrent.atomic.AtomicBoolean
import org.rsmod.api.db.jdbc.EmbeddedSameInstancePostgres
import org.rsmod.api.net.central.OpenRuneCentralWorldLink
import org.rsmod.api.net.central.embed.CentralEmbeddedLifecycle
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.server.services.Service
import org.rsmod.server.services.ServiceManager

class GameBootstrap @Inject constructor(
    services: Set<Service>,
    private val serverConfig: ServerConfig,
    private val centralEmbedded: CentralEmbeddedLifecycle,
    private val openRuneCentral: OpenRuneCentralWorldLink,
) {
    private val serviceManager = ServiceManager.create(services)

    suspend fun startupUntilReady(): Thread {
        EmbeddedSameInstancePostgres.ensureStarted(serverConfig)
        try {
            centralEmbedded.startIfConfigured()
            openRuneCentral.startInboundWatch()
            val startupResult = serviceManager.awaitStartup()
            if (startupResult is ServiceManager.StartResult.Error) {
                throw startupResult.throwable
            }
            val shutdownHook = Thread(::shutdown, "ShutdownHook")
            Runtime.getRuntime().addShutdownHook(shutdownHook)
            return shutdownHook
        } catch (t: Throwable) {
            runCatching { centralEmbedded.stopIfRunning() }
            EmbeddedSameInstancePostgres.stop()
            throw t
        }
    }

    fun awaitShutdown(shutdownHook: Thread) {
        try {
            serviceManager.awaitShutdownOrThrow()
        } finally {
            try {
                Runtime.getRuntime().removeShutdownHook(shutdownHook)
            } catch (_: IllegalStateException) {
                // Virtual machine is already in the process of shutting down - can safely noop.
            }
        }
    }

    suspend fun startup() {
        val hook = startupUntilReady()
        awaitShutdown(hook)
    }

    private fun shutdown() {
        val completed = AtomicBoolean(false)
        Thread(
                {
                    try {
                        Thread.sleep(FORCE_EXIT_AFTER_MS)
                        if (!completed.get()) {
                            EmbeddedSameInstancePostgres.forceStopNow()
                            Runtime.getRuntime().halt(1)
                        }
                    } catch (_: InterruptedException) {
                        // Shutdown finished before the watchdog fired.
                    }
                },
                "shutdown-watchdog",
            )
            .apply {
                isDaemon = true
                start()
            }
        try {
            serviceManager.shutdown()
            serviceManager.awaitShutdownOrThrow(
                signalTimeoutSecs = HOOK_TIMEOUT_SECS,
                cleanupTimeoutSecs = HOOK_TIMEOUT_SECS,
                shutdownTimeoutSecs = HOOK_TIMEOUT_SECS,
            )
            runCatching { centralEmbedded.stopIfRunning() }
            EmbeddedSameInstancePostgres.stop()
        } catch (_: Throwable) {
            EmbeddedSameInstancePostgres.forceStopNow()
        } finally {
            completed.set(true)
        }
    }

    private fun ServiceManager.awaitShutdownOrThrow(
        signalTimeoutSecs: Int = 30,
        cleanupTimeoutSecs: Int = 30,
        shutdownTimeoutSecs: Int = 30,
    ) {
        val result =
            awaitShutdown(
                signalTimeoutSecs = signalTimeoutSecs,
                cleanupTimeoutSecs = cleanupTimeoutSecs,
                shutdownTimeoutSecs = shutdownTimeoutSecs,
            )
        if (result is ServiceManager.ShutdownResult.Report && result.errors.isNotEmpty()) {
            throw result.errors.first()
        }
    }

    private companion object {
        private const val HOOK_TIMEOUT_SECS = 5
        private const val FORCE_EXIT_AFTER_MS = 20_000L
    }
}
