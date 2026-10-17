package org.rsmod.api.db.jdbc

import jakarta.inject.Provider
import java.lang.reflect.Proxy
import java.sql.Connection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameDatabaseTransactionTest {
    @Test
    fun `blocking and suspending transactions do not interleave on the shared connection`() {
        val open = AtomicInteger()
        val maxOpen = AtomicInteger()
        val commits = AtomicInteger()
        val database = database(fakeConnection(commits))
        val start = CountDownLatch(1)

        fun body() {
            maxOpen.accumulateAndGet(open.incrementAndGet(), ::maxOf)
            Thread.sleep(2)
            open.decrementAndGet()
        }

        val gameThread = thread {
            start.await()
            repeat(ROUNDS) { database.withTransactionBlocking { body() } }
        }
        val saver = thread {
            start.await()
            runBlocking { repeat(ROUNDS) { database.withTransaction { body() } } }
        }
        start.countDown()
        gameThread.join()
        saver.join()

        assertEquals(1, maxOpen.get())
        assertEquals(ROUNDS * 2, commits.get())
    }

    private fun database(connection: Connection): GameDatabase {
        val database =
            GameDatabase(
                Provider { error("Not used without connect().") },
                Provider { error("Not used without connect().") },
            )
        GameDatabase::class.java.getDeclaredField("connection").apply {
            isAccessible = true
            set(database, connection)
        }
        return database
    }

    private fun fakeConnection(commits: AtomicInteger): Connection =
        Proxy.newProxyInstance(
            javaClass.classLoader,
            arrayOf(Connection::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "isClosed" -> false
                "getAutoCommit" -> false
                "commit" -> {
                    commits.incrementAndGet()
                    null
                }
                "rollback" -> null
                else -> throw UnsupportedOperationException(method.name)
            }
        } as Connection

    private companion object {
        private const val ROUNDS = 50
    }
}
