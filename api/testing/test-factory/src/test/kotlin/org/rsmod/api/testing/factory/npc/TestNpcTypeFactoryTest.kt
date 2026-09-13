package org.rsmod.api.testing.factory.npc

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.rsmod.api.testing.factory.npcTypeFactory

class TestNpcTypeFactoryTest {
    @AfterEach
    fun clearOverlay() {
        ServerCacheManager.clearTestOverrides()
    }

    @Test
    fun `created npc type is registered in the cache manager overlay`() {
        val type = npcTypeFactory.create(id = 12345) { name = "Test goblin" }

        assertEquals("Test goblin", ServerCacheManager.getNpc(12345)?.name)
        assertEquals(type, ServerCacheManager.getNpc(12345))
    }
}
