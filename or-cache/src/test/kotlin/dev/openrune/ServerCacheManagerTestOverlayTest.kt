package dev.openrune

import dev.openrune.types.NpcServerType
import dev.openrune.types.ObjectServerType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ServerCacheManagerTestOverlayTest {
    @AfterEach
    fun clearOverlay() {
        ServerCacheManager.clearTestOverrides()
    }

    @Test
    fun `registered test object is visible on the same thread`() {
        val obj = ObjectServerType(id = 99999, name = "Test sword")
        ServerCacheManager.registerTestObject(obj)

        assertEquals("Test sword", ServerCacheManager.getObject(99999)?.name)
    }

    @Test
    fun `registered test npc is visible on the same thread`() {
        val npc = NpcServerType(id = 99998, name = "Test goblin")
        ServerCacheManager.registerTestNpc(npc)

        assertEquals("Test goblin", ServerCacheManager.getNpc(99998)?.name)
    }

    @Test
    fun `clearTestOverrides removes overlay entries for the calling thread`() {
        ServerCacheManager.registerTestObject(ObjectServerType(id = 99997, name = "Temp"))
        ServerCacheManager.clearTestOverrides()

        assertNull(ServerCacheManager.getObject(99997))
    }

    @Test
    fun `overlay entries are not visible from a different thread`() {
        ServerCacheManager.registerTestObject(ObjectServerType(id = 99996, name = "Thread local"))

        val latch = CountDownLatch(1)
        var seenFromOtherThread: ObjectServerType? = ObjectServerType(id = -1)
        val executor = Executors.newSingleThreadExecutor()
        executor.submit {
            seenFromOtherThread = ServerCacheManager.getObject(99996)
            latch.countDown()
        }
        latch.await(5, TimeUnit.SECONDS)
        executor.shutdown()

        assertNull(seenFromOtherThread)
    }
}
