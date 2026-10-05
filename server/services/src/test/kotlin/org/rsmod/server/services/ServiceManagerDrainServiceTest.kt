package org.rsmod.server.services

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.rsmod.server.services.concurrent.ScheduledDrainService
import org.rsmod.server.services.concurrent.ScheduledListenerService

class ServiceManagerDrainServiceTest {
    @Test
    fun `a drain service keeps running while listeners handle the shutdown signal`() {
        val saver = FakeSaver()
        val game = FakeGame(saver)
        val manager = ServiceManager.create(linkedSetOf(saver, game))

        assertEquals(ServiceManager.StartResult.Healthy, runBlocking { manager.awaitStartup() })
        manager.shutdown()
        val result = manager.awaitShutdown(2, 2, 2)

        assertEquals(ServiceManager.ShutdownResult.Clean, result)
        assertTrue(game.logoutsSaved)
    }

    private class FakeSaver : ScheduledDrainService {
        val runs = AtomicInteger()

        override suspend fun run() {
            runs.incrementAndGet()
            delay(5)
        }

        override fun createExecutor(): ExecutorService = Executors.newSingleThreadExecutor()

        override suspend fun setup() {}

        override suspend fun startup() {}

        override suspend fun shutdown() {}
    }

    private class FakeGame(private val saver: FakeSaver) : ScheduledListenerService {
        @Volatile var logoutsSaved = false

        override suspend fun signalShutdown() {
            val runsAtSignal = saver.runs.get()
            val saved =
                withTimeoutOrNull(1_000) {
                    while (saver.runs.get() < runsAtSignal + 3) {
                        delay(5)
                    }
                }
            logoutsSaved = saved != null
        }

        override suspend fun signalStartup() {}

        override suspend fun run() {
            delay(5)
        }

        override fun createExecutor(): ExecutorService = Executors.newSingleThreadExecutor()

        override suspend fun setup() {}

        override suspend fun startup() {}

        override suspend fun shutdown() {}
    }
}
