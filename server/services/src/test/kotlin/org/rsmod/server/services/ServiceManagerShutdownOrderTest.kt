package org.rsmod.server.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ServiceManagerShutdownOrderTest {
    @Test
    fun `resource services shut down after the services that still use them`() {
        val database = FakeDatabase()
        val saver = FakeAccountSaver(database)
        val manager = ServiceManager.create(linkedSetOf(database, saver))

        val started = runBlocking { manager.awaitStartup() }
        assertEquals(ServiceManager.StartResult.Healthy, started)

        manager.shutdown()
        val result = manager.awaitShutdown(1, 1, 5)

        assertEquals(ServiceManager.ShutdownResult.Clean, result)
        assertEquals(listOf("saved alice", "saved bob", "database closed"), database.log)
    }

    @Test
    fun `a failing service still lets the resources close`() {
        val database = FakeDatabase()
        val manager = ServiceManager.create(linkedSetOf(database, FailingService()))

        runBlocking { manager.awaitStartup() }
        manager.shutdown()
        val result = manager.awaitShutdown(1, 1, 5)

        assertEquals(1, (result as ServiceManager.ShutdownResult.Report).errors.size)
        assertEquals(listOf("database closed"), database.log)
    }

    private class FakeDatabase : ResourceService {
        val log = mutableListOf<String>()

        @Volatile private var open = false

        fun write(entry: String) {
            check(open) { "Connection is closed." }
            synchronized(log) { log += entry }
        }

        override suspend fun startup() {
            open = true
        }

        override suspend fun shutdown() {
            open = false
            synchronized(log) { log += "database closed" }
        }
    }

    private class FakeAccountSaver(private val database: FakeDatabase) : Service {
        override suspend fun startup() {}

        override suspend fun shutdown() {
            for (player in listOf("alice", "bob")) {
                delay(20)
                database.write("saved $player")
            }
        }
    }

    private class FailingService : Service {
        override suspend fun startup() {}

        override suspend fun shutdown() {
            error("Shutdown failed.")
        }
    }
}
