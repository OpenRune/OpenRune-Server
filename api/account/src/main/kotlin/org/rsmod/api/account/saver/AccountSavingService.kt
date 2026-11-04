package org.rsmod.api.account.saver

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.min
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import org.rsmod.api.account.character.main.CharacterAccountRepository
import org.rsmod.api.account.saver.request.AccountSaveRequest
import org.rsmod.api.account.saver.request.AccountSaveResponse
import org.rsmod.api.db.jdbc.GameDatabase
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.server.services.concurrent.ScheduledDrainService

// Serialized account saves: one writer at a time avoids overlapping transactions on the game DB.
// Therefore, save operations are not parallelized and instead run on a single thread.
public class AccountSavingService
@Inject
constructor(
    private val database: GameDatabase,
    private val repository: CharacterAccountRepository,
    private val serverConfig: ServerConfig,
) : ScheduledDrainService {
    private val logger = InlineLogger()

    private val pendingRequests = ConcurrentLinkedQueue<AccountSaveRequest>()

    private val pendingBatch = ArrayDeque<AccountSaveRequest>(REQUESTS_PER_BATCH)
    private var consecutiveFailureCount = AtomicInteger(0)

    public fun queue(request: AccountSaveRequest) {
        pendingRequests.add(request)
    }

    override suspend fun run() {
        val failureBackoffEnabled = consecutiveFailureCount.get() >= MAX_CONSECUTIVE_FAILURES
        val batchPollCount = if (failureBackoffEnabled) 1 else REQUESTS_PER_BATCH

        batchPendingRequests(batchPollCount)
        handlePendingBatch()

        if (!failureBackoffEnabled) {
            val backlog = pendingRequests.isNotEmpty()
            delay(if (backlog) DELAY_WHEN_BACKLOG_MS else DELAY_PER_BATCH)
            return
        }

        val backoffDelay = ERROR_BACKOFF_MS
        logger.warn { "Too many consecutive failures, backing off for: ${backoffDelay}ms" }
        delay(backoffDelay)
    }

    private fun batchPendingRequests(count: Int) {
        for (i in 0 until count) {
            val request = pendingRequests.poll() ?: break
            pendingBatch += request
        }
    }

    private suspend fun handlePendingBatch() {
        while (pendingBatch.isNotEmpty()) {
            handleRequestWithTimeout(pendingBatch.first(), REQUEST_TIMEOUT_MS)
            pendingBatch.removeFirst()
        }
    }

    private suspend fun handleRequestWithTimeout(request: AccountSaveRequest, timeoutMillis: Long) {
        while (true) {
            try {
                val result = withTimeoutOrNull(timeoutMillis) {
                    saveSegments(request)
                    true
                }
                if (result != null) {
                    invokeSaveCallback(request)
                    resetConsecutiveFailures()
                    return
                }
                logger.warn { "Account save timed out for character: ${request.characterId}" }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.warn(e) { "Could not save character: ${request.characterId}" }
            }
            incrementConsecutiveFailures()
            if (!incrementSaveAttempts(request)) {
                return
            }
        }
    }

    private suspend fun saveSegments(request: AccountSaveRequest) {
        database.withSchemaTransaction(request.worldType.key) { connection ->
            repository.save(
                connection,
                request.account,
                request.characterId,
                serverConfig.world,
            )
            for (segment in request.segments) {
                segment.save(connection, request.characterId)
            }
        }
    }

    private fun invokeSaveCallback(request: AccountSaveRequest) {
        safeInvokeCallback(request, AccountSaveResponse.Success(request.player))
    }

    override fun createExecutor(): ExecutorService {
        val threadFactory = ThreadFactory { runnable ->
            Thread(runnable, SERVICE_THREAD_NAME).apply { isDaemon = true }
        }
        return Executors.newSingleThreadExecutor(threadFactory)
    }

    override suspend fun setup() {
        logger.debug {
            "Account saving service accepting $REQUESTS_PER_BATCH requests / $DELAY_PER_BATCH ms."
        }
    }

    override suspend fun startup() {}

    override suspend fun shutdown() {
        logger.info { "Attempting to shutdown account saving service." }
        try {
            handleShutdownRequests()
            pendingBatch.clear()
            logger.info { "Account saving service successfully shut down." }
        } catch (t: Throwable) {
            logger.error(t) { "Account saving service failed to shut down." }
        }
    }

    private suspend fun handleShutdownRequests() {
        var saveCount = 0
        val result =
            withTimeoutOrNull(SHUTDOWN_TIMEOUT_MS) {
                while (isActive) {
                    val request = nextPendingRequest() ?: break
                    handleRequestWithTimeout(request, SHUTDOWN_REQUEST_TIMEOUT_MS)
                    removePendingRequest()
                    saveCount++
                }
            }

        if (saveCount > 0) {
            logger.info { "Saved $saveCount pending requests before shut down." }
        }

        // If `result` did not time out, we can return early. If it failed due to a timeout,
        // we switch to an "emergency mode" where we inform the remaining callbacks about
        // the internal server error so they can handle it appropriately.
        if (result != null) {
            return
        }

        logger.error { "Force-save failed due to timeout. Rejecting all pending requests..." }

        val leftover = pendingBatch.size + pendingRequests.size
        val emergencyRejection =
            withTimeoutOrNull(SHUTDOWN_EMERGENCY_TIMEOUT_MS) { rejectPendingRequests() }

        if (emergencyRejection != null) {
            logger.info { "Sent out $leftover pending request max-retry responses." }
            return
        }

        val usernames =
            (pendingBatch.asSequence() + pendingRequests.asSequence())
                .take(100).joinToString(",") { it.account.displayName }
        logger.error {
            "Emergency shutdown measure timed out! " +
                "(${pendingBatch.size + pendingRequests.size} pending requests, usernames=$usernames)"
        }
    }

    private fun nextPendingRequest(): AccountSaveRequest? =
        pendingBatch.firstOrNull() ?: pendingRequests.peek()

    private fun removePendingRequest() {
        if (pendingBatch.isNotEmpty()) {
            pendingBatch.removeFirst()
        } else {
            pendingRequests.poll()
        }
    }

    /** Rejects remaining saves, including any batch interrupted during shutdown. */
    private fun rejectPendingRequests() {
        while (true) {
            val request = nextPendingRequest() ?: break
            val response = AccountSaveResponse.InternalShutdownError(request.player)
            safeInvokeCallback(request, response)
            removePendingRequest()
        }
    }

    private fun incrementSaveAttempts(request: AccountSaveRequest): Boolean {
        if (request.attempts++ < REQUEST_RETRIES_BEFORE_FAILURE) {
            return true
        }
        safeInvokeCallback(request, AccountSaveResponse.ExcessiveRetries(request.player))
        logger.error { "Reached max retry attempts for character: ${request.characterId}" }
        return false
    }

    // Ideally, any errors would be handled within the callback itself. However, since this is
    // executed on the service thread, we add a safeguard to prevent one faulty callback from
    // affecting other pending requests.
    private fun safeInvokeCallback(request: AccountSaveRequest, response: AccountSaveResponse) {
        try {
            request.callback(response)
        } catch (e: Exception) {
            val message = "Error handling save callback for character: ${request.characterId}"
            logger.error(e) { message }
        }
    }

    private fun incrementConsecutiveFailures() {
        consecutiveFailureCount.updateAndGet { curr -> min(curr + 1, MAX_CONSECUTIVE_FAILURES) }
    }

    private fun resetConsecutiveFailures() {
        val previous = consecutiveFailureCount.getAndSet(0)
        if (previous == 0) {
            return
        }
        logger.info { "Account saving service recovered after $previous failure(s)." }
    }

    private companion object {
        private const val SERVICE_THREAD_NAME = "account-writer"

        /**
         * The time (in ms) to wait between each batch cycle when not under failure backoff and the
         * queue is empty. Avoids a hot loop when nothing is queued.
         */
        private const val DELAY_PER_BATCH = 100L

        /** When saves are already waiting, poll again quickly so work reaches the DB sooner. */
        private const val DELAY_WHEN_BACKLOG_MS = 1L

        /** The maximum number of requests processed per service cycle ([run]). */
        private const val REQUESTS_PER_BATCH = 25

        /**
         * The number of times a request will retry before returning a failure response to the
         * callback. The caller is responsible for any backup or recovery strategy.
         *
         * This is compared against [AccountSaveRequest.attempts].
         */
        private const val REQUEST_RETRIES_BEFORE_FAILURE = 3

        /**
         * The timeout (in ms) before a request is considered failed and its attempt count
         * ([AccountSaveRequest.attempts]) is incremented.
         *
         * If the request still has remaining retries, it will be re-queued for another attempt.
         */
        private const val REQUEST_TIMEOUT_MS = 5000L

        /**
         * Similar to [REQUEST_TIMEOUT_MS], but used during the [shutdown] process when attempting
         * to hastily process the pending requests.
         */
        private const val SHUTDOWN_REQUEST_TIMEOUT_MS = 2000L

        /**
         * The delay duration (in ms) when backing off due to consecutive failures. Gives external
         * systems time to recover (e.g. Database under load).
         */
        private const val ERROR_BACKOFF_MS = 10_000L

        /**
         * The maximum number of consecutive batch failures before the service enters a backoff
         * period.
         *
         * During backoff, the service delays each cycle using [ERROR_BACKOFF_MS]. This failure
         * state persists until at least one request is successfully processed.
         */
        private const val MAX_CONSECUTIVE_FAILURES = 10

        /**
         * A safeguard timeout to prevent the application from waiting indefinitely during service
         * shutdown.
         *
         * While all pending requests are expected to eventually complete, or invoke their callback
         * with a "failed after X attempts" response, this timeout ensures the shutdown process does
         * not hang in case something goes wrong.
         */
        private const val SHUTDOWN_TIMEOUT_MS = 30_000L

        /**
         * A secondary safeguard timeout used after [SHUTDOWN_TIMEOUT_MS] during service shutdown.
         *
         * If any requests remain after the initial timeout, this ensures they forcibly invoke their
         * [AccountSaveResponse.InternalShutdownError] callback. This allows any external backup or
         * recovery strategies to proceed.
         */
        private const val SHUTDOWN_EMERGENCY_TIMEOUT_MS = 30_000L
    }
}
