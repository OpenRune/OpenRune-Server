package org.rsmod.api.net.rsprot.websocket.tls

import com.github.michaelbull.logging.InlineLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileReader
import java.io.FileWriter
import java.net.URI
import java.security.KeyPair
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rsmod.api.server.config.WebSocketTlsConfig
import org.shredzone.acme4j.Account
import org.shredzone.acme4j.AccountBuilder
import org.shredzone.acme4j.Authorization
import org.shredzone.acme4j.Order
import org.shredzone.acme4j.Session
import org.shredzone.acme4j.Status
import org.shredzone.acme4j.challenge.Http01Challenge
import org.shredzone.acme4j.util.CSRBuilder
import org.shredzone.acme4j.util.KeyPairUtils

/**
 * Obtains and renews a TLS certificate for [WebSocketTlsConfig.domain] from an ACME CA (Let's
 * Encrypt by default, see [WebSocketTlsConfig.caDirectoryUrl]) via the HTTP-01 challenge.
 *
 * Follows acme4j's own documented account/order/authorize/finalize flow. The account key pair and
 * the certificate's own key pair are generated once and persisted under
 * [WebSocketTlsConfig.certDir] (PEM, PKCS#8 — the same format Netty's `SslContextBuilder.forServer`
 * reads directly, no conversion needed) so a restart reuses the same ACME account instead of
 * registering a new one, and the resulting certificate chain is written there too so a restart
 * doesn't request a fresh certificate it doesn't need yet — Let's Encrypt rate-limits how often the
 * same domain can be (re)issued.
 */
internal class AcmeCertificateProvisioner(private val config: WebSocketTlsConfig) {
    private val logger = InlineLogger()
    private val certDir = File(config.certDir).apply { mkdirs() }
    private val accountKeyFile = File(certDir, "account-key.pem")
    private val domainKeyFile = File(certDir, "domain-key.pem")
    private val certChainFile = File(certDir, "cert-chain.pem")

    /** The cert chain + key files Netty needs, or null if there's nothing on disk yet, or what's there is within [RENEWAL_THRESHOLD_DAYS] of expiring. */
    fun currentCertificateFiles(): CertificateFiles? {
        if (!certChainFile.exists() || !domainKeyFile.exists()) return null
        val cert = readCertificate() ?: return null
        val daysLeft = Duration.between(Instant.now(), cert.notAfter.toInstant()).toDays()
        if (daysLeft <= RENEWAL_THRESHOLD_DAYS) return null
        return CertificateFiles(certChainFile, domainKeyFile)
    }

    /** Requests a fresh certificate from the ACME CA, overwriting whatever's on disk. */
    suspend fun provision(): CertificateFiles =
        withContext(Dispatchers.IO) {
            val domain = requireNotNull(config.domain) { "websocket.tls.domain must be set to use automatic TLS." }
            val email = requireNotNull(config.email) { "websocket.tls.email must be set to use automatic TLS (required by the CA)." }

            logger.info { "Requesting a TLS certificate for '$domain' via ${config.caDirectoryUrl}..." }
            val session = Session(URI.create(config.caDirectoryUrl))
            val account = findOrRegisterAccount(session, email)
            val domainKeyPair = loadOrCreateKeyPair(domainKeyFile)

            val order = account.newOrder().domains(domain).create()
            for (authorization in order.authorizations) {
                if (authorization.status != Status.VALID) {
                    authorizeDomain(authorization)
                }
            }

            val csr =
                CSRBuilder().apply {
                    addDomain(domain)
                    sign(domainKeyPair)
                }
            order.execute(csr.encoded)
            val finalStatus = order.waitForCompletion(ORDER_TIMEOUT)
            if (finalStatus != Status.VALID) {
                error("Certificate order for '$domain' ended in status $finalStatus instead of valid.")
            }

            val certificate = requireNotNull(order.certificate) { "ACME order completed without a certificate." }
            FileWriter(certChainFile).use { certificate.writeCertificate(it) }
            logger.info { "TLS certificate for '$domain' issued, expires ${certificate.certificate.notAfter}." }
            CertificateFiles(certChainFile, domainKeyFile)
        }

    private fun findOrRegisterAccount(session: Session, email: String): Account {
        val accountKeyPair = loadOrCreateKeyPair(accountKeyFile)
        return AccountBuilder()
            .addContact("mailto:$email")
            .agreeToTermsOfService()
            .useKeyPair(accountKeyPair)
            .create(session)
    }

    private fun loadOrCreateKeyPair(file: File): KeyPair {
        if (file.exists()) {
            return FileReader(file).use { KeyPairUtils.readKeyPair(it) }
        }
        val keyPair = KeyPairUtils.createKeyPair(2048)
        FileWriter(file).use { KeyPairUtils.writeKeyPair(keyPair, it) }
        return keyPair
    }

    private suspend fun authorizeDomain(authorization: Authorization) {
        val challenge =
            authorization.findChallenge(Http01Challenge::class.java).orElse(null)
                ?: error("CA did not offer an HTTP-01 challenge for '${authorization.identifier.domain}'.")
        if (challenge.status == Status.VALID) return

        val server = AcmeHttpChallengeServer()
        server.serveWhile(challenge.token, challenge.authorization, HTTP_CHALLENGE_HOST) {
            challenge.trigger()
            val finalStatus = challenge.waitForCompletion(CHALLENGE_TIMEOUT)
            if (finalStatus != Status.VALID) {
                error("HTTP-01 challenge for '${authorization.identifier.domain}' ended in status $finalStatus instead of valid.")
            }
        }
    }

    private fun readCertificate(): X509Certificate? =
        runCatching {
            FileInputStream(certChainFile).use {
                CertificateFactory.getInstance("X.509").generateCertificate(it) as X509Certificate
            }
        }.getOrNull()

    internal data class CertificateFiles(val certChainFile: File, val keyFile: File)

    private companion object {
        /** Renew once a certificate has this many days or fewer left — comfortably ahead of Let's Encrypt's 90-day lifetime. */
        private const val RENEWAL_THRESHOLD_DAYS = 30L
        private const val HTTP_CHALLENGE_HOST = "0.0.0.0"
        private val CHALLENGE_TIMEOUT: Duration = Duration.ofSeconds(60)
        private val ORDER_TIMEOUT: Duration = Duration.ofSeconds(60)
    }
}
