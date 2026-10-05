package org.rsmod.api.testing.factory.npc

import dev.openrune.ServerCacheManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.factory.TestCacheTypes
import org.rsmod.api.testing.factory.npcTypeFactory

class TestNpcTypeFactoryTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun installStub() {
            TestCacheTypes.install()
        }
    }

    @Test
    fun `created npc type resolves through the cache manager`() {
        val type = npcTypeFactory.create { name = "Test goblin" }

        assertSame(type, ServerCacheManager.getNpc(type.id))
    }

    @Test
    fun `created npc type resolves through the or-default lookup`() {
        val type = npcTypeFactory.create()

        assertSame(type, ServerCacheManager.getNpcOrDefault(type.id))
    }

    @Test
    fun `created npc type is included in the bulk lookup`() {
        val type = npcTypeFactory.create()
        val npcs = ServerCacheManager.getNpcs()

        assertTrue(type.id in npcs)
        assertSame(type, npcs[type.id])
        assertTrue(npcs.values.any { it === type })
    }

    @Test
    fun `created npc type is not treated as a real cache entry`() {
        val type = npcTypeFactory.create()

        assertFalse(TestCacheTypes.npcs.isCacheId(type.id))
        TestCacheTypes.npcs.registerIfSynthetic(type.id, type)
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
