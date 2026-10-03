package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.CombatEffects
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.impl.NpcPoison
import org.rsmod.api.random.GameRandom
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.*

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class TentacleSpecialEffectsTest {
    @Test fun `misses still freeze for eight ticks and can poison for four only at impact`() {
        val source = source()
        val target = npc()
        val rng = mock(GameRandom::class.java)
        `when`(rng.of(100)).thenReturn(99)
        `when`(rng.of(2)).thenReturn(0)
        val hit = hit()
        TentacleSpecialEffects.attach(hit, source, target, rng)
        assertFalse(CombatEffects.isFrozen(target))
        assertFalse(NpcPoison.isPoisoned(target))
        hit.impactEffects.complete(0)
        assertEquals(108, target.vars["varn.freeze_end_clock"])
        assertTrue(NpcPoison.isPoisoned(target))
        assertEquals(4, (target.queueList.iterator()!!.next().args as Hit).damage)
        hit.impactEffects.complete(0)
        verify(rng, times(1)).of(2)
    }

    @Test fun `freeze and poison immunities are independent and existing freeze is not extended`() {
        val source = source()
        val rng = mock(GameRandom::class.java)
        `when`(rng.of(100)).thenReturn(99)
        `when`(rng.of(2)).thenReturn(0)
        val immune = npc(100, true)
        val hit = hit()
        TentacleSpecialEffects.attach(hit, source, immune, rng)
        hit.impactEffects.complete(10)
        assertFalse(CombatEffects.isFrozen(immune))
        assertFalse(NpcPoison.isPoisoned(immune))
        val already = npc()
        CombatEffects.freeze(already, 32)
        val second = hit()
        TentacleSpecialEffects.attach(second, source, already, rng)
        second.impactEffects.complete(0)
        assertEquals(132, already.vars["varn.freeze_end_clock"])
        assertTrue(NpcPoison.isPoisoned(already))
    }

    @Test fun `cancelled dead-target and old-login hits cannot apply the effect`() {
        val source = source()
        val rng = mock(GameRandom::class.java)
        val target = npc()
        TentacleSpecialEffects.attach(hit(), source, target, rng)
        assertFalse(CombatEffects.isFrozen(target))
        val dead = hit()
        TentacleSpecialEffects.attach(dead, source, target, rng)
        target.hitpoints = 0
        dead.impactEffects.complete(10)
        target.hitpoints = 100
        val stale = hit()
        TentacleSpecialEffects.attach(stale, source, target, rng)
        source.uuid = 9; source.assignUid()
        stale.impactEffects.complete(10)
        assertFalse(CombatEffects.isFrozen(target))
        verifyNoInteractions(rng)
    }

    @Test fun `player miss freezes and losing poison roll leaves poison absent`() {
        val source = source()
        val target = Player().apply {
            slotId = 2; uuid = 2; assignUid(); currentMapClock = 100
            statMap.setCurrentLevel("stat.hitpoints", 99)
        }
        val rng = mock(GameRandom::class.java)
        `when`(rng.of(2)).thenReturn(1)
        val hit = hit()
        TentacleSpecialEffects.attach(hit, source, target, rng)
        hit.impactEffects.complete(0)
        assertTrue(target.isFrozen)
        assertEquals(0, target.vars["varp.poison_severity"])
        verify(rng).of(2)
        verify(rng, never()).of(100)
    }

    private fun source() = Player().apply { slotId = 1; uuid = 1; assignUid() }
    private fun npc(resistance: Int = 0, poisonImmune: Boolean = false): Npc {
        val base = ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }
        val type = base.copy(paramsRaw = base.paramsRaw.orEmpty().toMutableMap().apply {
            put(params.freeze_resistance.id, resistance)
            put(params.poison_immunity.id, if (poisonImmune) 1 else 0)
        })
        type.paramMap = dev.openrune.ParamMap(checkNotNull(type.paramsRaw))
        return Npc(type).apply { slotId = 2; assignUid(); currentMapClock = 100; hitpoints = 100 }
    }
    private fun hit() = Hit(HitType.Melee, Hitmark(0), null, null, null)
    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
