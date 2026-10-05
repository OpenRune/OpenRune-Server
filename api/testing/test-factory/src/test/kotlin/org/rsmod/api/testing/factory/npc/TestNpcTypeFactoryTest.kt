package org.rsmod.api.testing.factory.npc

import dev.openrune.ServerCacheManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.factory.npcTypeFactory

class TestNpcTypeFactoryTest {
    @Test
    fun `created npc type resolves through the cache manager`() {
        val type = npcTypeFactory.create { name = "Test goblin" }

        assertSame(type, ServerCacheManager.getNpc(type.id))
    }

    @Test
    fun `created npc types get distinct ids`() {
        val first = npcTypeFactory.create()
        val second = npcTypeFactory.create()

        assertNotEquals(first.id, second.id)
    }

    @Test
    fun `created npc type resolves from another thread`() {
        val type = npcTypeFactory.create()

        val executor = Executors.newSingleThreadExecutor()
        val resolved = executor.submit<Any?> { ServerCacheManager.getNpc(type.id) }
        executor.shutdown()

        assertSame(type, resolved.get(5, TimeUnit.SECONDS))
    }
}
