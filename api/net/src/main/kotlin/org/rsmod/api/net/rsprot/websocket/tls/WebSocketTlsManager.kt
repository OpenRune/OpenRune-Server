package org.rsmod.api.net.rsprot.websocket.tls

import com.github.michaelbull.logging.InlineLogger
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.rsmod.api.server.config.WebSocketTlsConfig

/**
 * Keeps an [SslContext] ready for the WebSocket bridge's TLS listener, either loaded from an
 * operator-supplied certificate ([WebSocketTlsConfig.certPath]/[WebSocketTlsConfig.keyPath]) or
 * obtained/renewed automatically from an ACME CA (see [AcmeCertificateProvisioner]) — whichever
 * the config asks for. [current] is read fresh for every new connection (see
 * `WebSocketBridgeService`'s channel initializer), so a background renewal naturally takes effect
 * for the next connection without needing to touch any already-established one.
 */
internal class WebSocketTlsManager(private val config: WebSocketTlsConfig) {
    private val logger = InlineLogger()
    private val provisioner = AcmeCertificateProvisioner(config)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var renewalJob: Job? = null

    @Volatile private var current: SslContext? = null

    fun current(): SslContext? = current

    /**
     * Loads/obtains the initial certificate — must complete before the bridge binds its TLS
     * listener, so this is called synchronously (suspending) from `WebSocketBridgeService.startup`
     * rather than kicked off in the background. Starts the renewal loop afterward, only for the
     * ACME path — an operator-supplied certificate is never renewed by this process, that's on them.
     */
    suspend fun start() {
        current = loadOrObtainContext()
        if (config.certPath == null) {
            renewalJob = scope.launch { renewalLoop() }
        }
    }

    fun stop() {
        renewalJob?.cancel()
        scope.cancel()
    }

    private suspend fun renewalLoop() {
        while (true) {
            delay(RENEWAL_CHECK_INTERVAL_MS)
            try {
                val existing = provisioner.currentCertificateFiles()
                if (existing == null) {
                    logger.info { "WebSocket bridge TLS certificate is due for renewal, requesting a new one..." }
                    val files = provisioner.provision()
                    current = withContext(Dispatchers.IO) { buildContext(files.certChainFile, files.keyFile) }
                    logger.info { "WebSocket bridge TLS certificate renewed." }
                }
            } catch (t: Throwable) {
                logger.error(t) { "WebSocket bridge TLS renewal check failed; keeping the current certificate." }
            }
        }
    }

    private suspend fun loadOrObtainContext(): SslContext {
        val manualCertPath = config.certPath
        val manualKeyPath = config.keyPath
        if (manualCertPath != null && manualKeyPath != null) {
            logger.info { "WebSocket bridge TLS using operator-supplied certificate: $manualCertPath" }
            return withContext(Dispatchers.IO) { buildContext(File(manualCertPath), File(manualKeyPath)) }
        }

        val existing = provisioner.currentCertificateFiles()
        if (existing != null) {
            logger.info { "WebSocket bridge TLS reusing existing certificate for '${config.domain}'." }
            return withContext(Dispatchers.IO) { buildContext(existing.certChainFile, existing.keyFile) }
        }

        val files = provisioner.provision()
        return withContext(Dispatchers.IO) { buildContext(files.certChainFile, files.keyFile) }
    }

    private fun buildContext(certChainFile: File, keyFile: File): SslContext =
        SslContextBuilder.forServer(certChainFile, keyFile).build()

    private companion object {
        /** How often the background loop checks whether renewal is due — not how often it actually renews; [AcmeCertificateProvisioner.currentCertificateFiles] only returns null once genuinely within the renewal window. */
        private const val RENEWAL_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours
    }
}
