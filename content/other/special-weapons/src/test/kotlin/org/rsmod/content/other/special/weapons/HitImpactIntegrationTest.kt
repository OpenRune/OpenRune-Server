package org.rsmod.content.other.special.weapons

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.hit.processor.StandardNpcHitProcessor
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.hit.Hit
import org.rsmod.game.hit.HitType
import org.rsmod.game.hit.Hitmark

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class HitImpactIntegrationTest {
    @Test fun `native npc processor runs effect after hp deduction with capped damage`() {
        val npc = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }).apply { hitpoints = 7 }
        val access = mock(StandardNpcAccess::class.java)
        `when`(access.npc).thenReturn(npc)
        val hit = Hit(HitType.Magic, Hitmark(0).copy(damage = 50), null, null, null)
        var actual = -1
        var remaining = -1
        hit.impactEffects.add { actual = it; remaining = npc.hitpoints }
        with(StandardNpcHitProcessor(PlayerList(), EventBus(), emptySet())) { access.process(hit) }
        assertEquals(7, actual)
        assertEquals(0, remaining)
        hit.impactEffects.complete(50)
        assertEquals(7, actual)
    }
    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
