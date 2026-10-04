package org.rsmod.content.bosses.araxxor

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.rsmod.api.npc.access.StandardNpcAccess
import org.rsmod.api.npc.access.StandardNpcAccessContext
import org.rsmod.api.npc.hit.processor.StandardNpcHitProcessor
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PlayerList
import org.rsmod.game.entity.npc.NpcInfoProtocol
import org.rsmod.game.hit.*

@ResourceLock("ServerCacheManager")
class AraxxorImpactTest {
    @Test fun `unmodified zero marks are preserved and transformed zero marks are normalized`() {
        val npc = Npc(checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.BOSS.asRSCM())))
        val protocol = mock(NpcInfoProtocol::class.java)
        npc.infoProtocol = protocol
        val access = StandardNpcAccess(npc, mock(GameCoroutine::class.java), mock(StandardNpcAccessContext::class.java))
        val processor = StandardNpcHitProcessor(PlayerList(), mock(EventBus::class.java), emptySet())
        val original = Hit(HitType.Typeless, Hitmark(0).copy(self = 15, source = 15, public = 15, damage = 0), null, null, null)
        with(processor) { access.process(original) }
        verify(protocol).showHitmark(original.hitmark)
        val cancelled = original.copy(hitmark = original.hitmark.copy(damage = 50), impactEffects = HitImpactEffects())
        cancelled.impactEffects.beforeImpact { 0 }
        with(processor) { access.process(cancelled) }
        val zero = org.rsmod.api.config.refs.done.hitmark_groups.zero_damage
        verify(protocol).showHitmark(cancelled.hitmark.copy(damage = 0,
            self = zero.lit.asRSCM(), source = zero.lit.asRSCM(), public = zero.tint?.asRSCM()))
        assertEquals(1020, npc.hitpoints)
    }

    @Test fun `native npc processor transforms before hp cap and completes with applied damage`() {
        val npc = Npc(checkNotNull(ServerCacheManager.getNpc(AraxxorAssets.BOSS.asRSCM())))
        npc.hitpoints = 12
        val access = StandardNpcAccess(npc, mock(GameCoroutine::class.java), mock(StandardNpcAccessContext::class.java))
        val processor = StandardNpcHitProcessor(PlayerList(), mock(EventBus::class.java), emptySet())
        var preparations = 0
        val applied = mutableListOf<Int>()
        val hit = Hit(HitType.Typeless, Hitmark(0).copy(damage = 50), null, null, null)
        hit.impactEffects.beforeImpact { preparations++; it / 2 }
        hit.impactEffects.add { applied += it }
        with(processor) { access.process(hit) }
        assertEquals(1, preparations)
        assertEquals(0, npc.hitpoints)
        assertEquals(listOf(12), applied)
    }
    companion object { @JvmStatic @BeforeAll fun cache() { ServerCacheManager.init(240).close() } }
}
