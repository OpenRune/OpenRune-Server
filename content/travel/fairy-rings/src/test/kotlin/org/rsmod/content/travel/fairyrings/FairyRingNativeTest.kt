package org.rsmod.content.travel.fairyrings

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext
import org.rsmod.routefinder.collision.CollisionFlagMap

@ResourceLock("ServerCacheManager")
internal class FairyRingNativeTest {
    @BeforeEach fun cache() { ServerCacheManager.init(240).close() }

    @Test fun `native table contains every dial combination and native handlers register`() {
        val rings = FairyRings.load()
        assertEquals(64, rings.all.size)
        assertEquals(64, rings.all.map { it.code }.toSet().size)
        assertTrue(rings.all.count { it.destination != null } >= 50)
        for (r in rings.all) {
            assertEquals(r, rings.byDials(r.dials))
            assertEquals(r, rings.byLogIndex(r.logIndex))
        }
        with(FairyRingScript(PlayerTeleportValidator(emptySet()), mock(AreaChecker::class.java), CollisionFlagMap())) {
            ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
        }
    }
}
