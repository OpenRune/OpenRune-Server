package org.rsmod.content.other.special.weapons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.WeaponVenom
import org.rsmod.api.config.refs.params
import org.rsmod.api.mechanics.toxins.NpcVenomTimerScript
import org.rsmod.api.mechanics.toxins.impl.NpcPoison
import org.rsmod.api.mechanics.toxins.impl.NpcVenom
import org.rsmod.api.random.GameRandom
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class WeaponVenomTest {
    @Test fun `venom starts after thirty ticks increases to twenty and cannot be downgraded`() {
        val npc = npc()
        assertTrue(NpcVenom.tryVenom(npc))
        assertTrue(npc.queueList.isEmpty)
        assertEquals(30, npc.timerMap.extractInterval(npc.timerMap[NpcVenom.TIMER.asRSCM().toShort()]!!))
        assertFalse(NpcVenom.tryVenom(npc))
        assertFalse(NpcPoison.tryPoison(npc, 26))
        val hits = mutableListOf<Int>()
        repeat(10) {
            NpcVenom.tick(npc)
            hits.add((npc.queueList.iterator()!!.next().args as Hit).damage)
            npc.queueList.clear()
        }
        assertEquals(listOf(6, 8, 10, 12, 14, 16, 18, 20, 20, 20), hits)
        assertEquals(8, npc.vars[NpcVenom.STRIKES])
    }

    @Test fun `venom respects separate immunities and uses poison fallback`() {
        val immune = npc(poisonImmune = true)
        assertFalse(NpcVenom.tryVenom(immune))
        assertTrue(immune.queueList.isEmpty)
        val fallback = npc(venomImmune = true)
        assertTrue(NpcVenom.tryVenom(fallback))
        assertFalse(NpcVenom.isEnvenomed(fallback))
        assertTrue(NpcPoison.isPoisoned(fallback))
        assertEquals(6, (fallback.queueList.iterator()!!.next().args as Hit).damage)
    }

    @Test fun `respawn and death clear venom and timer id survives native signed short dispatch`() {
        val npc = npc().apply { slotId = 1; assignUid() }
        NpcVenom.tryVenom(npc)
        npc.setRespawnValues()
        NpcVenom.tick(npc)
        assertFalse(NpcVenom.isEnvenomed(npc))
        Assertions.assertNull(npc.timerMap[NpcVenom.TIMER.asRSCM().toShort()])
        NpcVenom.tryVenom(npc)
        npc.hitpoints = 0
        NpcVenom.tick(npc)
        assertFalse(NpcVenom.isEnvenomed(npc))
        assertFalse(NpcVenom.tryVenom(npc))
        assertTrue(NpcVenom.TIMER.asRSCM() in 0..Short.MAX_VALUE)
        with(NpcVenomTimerScript()) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
    }

    @Test fun `weapon only envenoms on successful impact and normal chance is one in four`() {
        val source = player()
        val random = mock(GameRandom::class.java)
        `when`(random.of(4)).thenReturn(1, 0)
        val target = npc()
        val zero = hit()
        WeaponVenom.attach(zero, source, target, random)
        zero.impactEffects.complete(0)
        verifyNoInteractions(random)
        val miss = hit()
        WeaponVenom.attach(miss, source, target, random)
        miss.impactEffects.complete(5)
        assertFalse(NpcVenom.isEnvenomed(target))
        val proc = hit()
        WeaponVenom.attach(proc, source, target, random)
        assertFalse(NpcVenom.isEnvenomed(target))
        proc.impactEffects.complete(5)
        assertTrue(NpcVenom.isEnvenomed(target))
        verify(random, times(2)).of(4)
    }

    @Test fun `charged serpent helmet variants guarantee npc venom without rng`() {
        for (helmet in listOf("obj.serpentine_helm_charged", "obj.serpentine_helm_charged_cyan", "obj.serpentine_helm_charged_red")) {
            val source = player()
            source.worn[Wearpos.Hat.slot] = InvObj(helmet)
            val target = npc()
            val random = mock(GameRandom::class.java)
            val hit = hit()
            WeaponVenom.attach(hit, source, target, random)
            hit.impactEffects.complete(3)
            assertTrue(NpcVenom.isEnvenomed(target))
            verifyNoInteractions(random)
        }
    }
    private fun npc(poisonImmune: Boolean = false, venomImmune: Boolean = false): Npc {
        val base = ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }
        val type = base.copy(paramsRaw = base.paramsRaw.orEmpty().toMutableMap().apply {
            put(params.poison_immunity.id, if (poisonImmune) 1 else 0)
            put(params.venom_immunity.id, if (venomImmune) 1 else 0)
        })
        type.paramMap = dev.openrune.ParamMap(checkNotNull(type.paramsRaw))
        return Npc(type).apply { hitpoints = 100 }
    }
    private fun player() = Player().apply { worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14)) }
    private fun hit() = Hit(HitType.Ranged, Hitmark(0).copy(damage = 10), null, null, null)
    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
