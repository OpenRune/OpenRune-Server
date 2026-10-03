package org.rsmod.content.other.special.attacks

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.formulas.accuracy.magic.NvNMagicAccuracy
import org.rsmod.api.config.refs.params
import org.rsmod.api.npc.MagicDefenceDrain
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.AyakCharges
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.content.other.special.attacks.magic.AyakSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.WorldQueueList

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class AyakSpecialAttackTest {
    @Test fun `soul rend uses base multiplier before gear and drains only the struck spawn on impact`() {
        val f = fixture()
        val other = Npc(f.target.type)
        val baseBonus = f.target.visType.param(params.defence_magic)
        val magicLevel = f.target.magicLvl
        assertEquals(500, f.special.energyInHundreds)
        assertTrue(complete { f.special.attack(f.access, f.target, f.attack) })
        verify(f.manager).setNextAttackDelay(f.access, 5)
        verify(f.manager).rollStaffAccuracy(f.access, f.target, null, 2.0)
        verify(f.manager).rollStaffMaxHit(f.access, f.target, 35, 1.0)
        verify(f.access).spotanim("spotanim.vfx_ayak_player_special_spotanim", 0, 0, 0)
        verify(f.target).spotanim("spotanim.vfx_ayak_impact_special_spotanim", 60, 0, 0)
        assertEquals(0, f.target.vars[MagicDefenceDrain.VAR])
        val delayed = f.queues.iterator().next()
        assertEquals(2, delayed.remainingCycles)
        delayed.action()
        assertEquals(23, f.target.vars[MagicDefenceDrain.VAR])
        assertEquals(0, other.vars[MagicDefenceDrain.VAR])
        assertEquals(baseBonus, f.target.visType.param(params.defence_magic))
        assertEquals(magicLevel, f.target.magicLvl)
        assertEquals(1, AyakCharges.count(f.player.worn[Wearpos.RightHand.slot]!!))
        val formula = NvNMagicAccuracy()
        assertTrue(formula.computeDefenceRoll(f.target.type, 99, 99, 23) < formula.computeDefenceRoll(f.target.type, 99, 99))
    }

    @Test fun `bonus drain has zero floor preserves negative base bonuses and clears on respawn`() {
        val f = fixture()
        val base = f.target.type.param(params.defence_magic)
        assertEquals(base, MagicDefenceDrain.apply(f.target, Int.MAX_VALUE))
        assertEquals(0, MagicDefenceDrain.apply(f.target, 100))
        assertEquals(0, MagicDefenceDrain.bonus(base, f.target.vars[MagicDefenceDrain.VAR]))
        assertEquals(-10, MagicDefenceDrain.bonus(-10, 20))
        f.target.setRespawnValues()
        assertEquals(0, f.target.vars[MagicDefenceDrain.VAR])
    }

    @Test fun `empty weapon and pvp return failure so no special energy is spent`() {
        val f = fixture(charges = 0)
        assertFalse(complete { f.special.attack(f.access, f.target, f.attack) })
        verify(f.manager).stopCombat(f.access)
        verifyNoMoreInteractions(f.manager)
        clearInvocations(f.manager)
        assertFalse(complete { f.special.attack(f.access, Player(), f.attack) })
        verify(f.manager).stopCombat(f.access)
        verifyNoMoreInteractions(f.manager)
        assertFalse(f.queues.iterator().hasNext())
    }

    @Test fun `splash spends final charge without drain and respawn invalidates pending effect`() {
        val splash = fixture(charges = 1, accurate = false)
        assertTrue(complete { splash.special.attack(splash.access, splash.target, splash.attack) })
        assertEquals(AyakCharges.EMPTY.asRSCM(), splash.player.worn[Wearpos.RightHand.slot]!!.id)
        assertFalse(splash.queues.iterator().hasNext())
        verify(splash.manager).stopCombat(splash.access)
        val f = fixture()
        assertTrue(complete { f.special.attack(f.access, f.target, f.attack) })
        f.target.lifecycleRespawnCycle = 20
        f.target.setRespawnValues()
        f.queues.iterator().next().action()
        assertEquals(0, f.target.vars[MagicDefenceDrain.VAR])
    }
    private data class Fixture(val player: Player, val access: ProtectedAccess, val manager: SpecialAttackManager, val target: Npc, val attack: CombatAttack.Staff, val queues: WorldQueueList, val special: SpecialAttack.Magic)
    private fun fixture(charges: Int = 2, accurate: Boolean = true): Fixture {
        val player = Player().apply {
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            statMap.setCurrentLevel("stat.magic", 99.toByte())
        }
        val item = if (charges == 0) InvObj(AyakCharges.CHARGED) else AyakCharges.write(InvObj(AyakCharges.EMPTY), charges)
        player.worn[Wearpos.RightHand.slot] = item
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(player)
        val manager = mock(SpecialAttackManager::class.java)
        val queues = WorldQueueList()
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }.also { it.isAccessible = true }.invoke(weapons)
        val registry = SpecialAttackRegistry(weapons)
        with(AyakSpecialAttack(queues)) { SpecialAttackRepository(registry).register(manager) }
        val target = spy(Npc(ServerCacheManager.getNpcs().values.first { it.param(params.defence_magic) > 40 })).apply {
            slotId = 1
            assignUid()
        }
        val attack = CombatAttack.Staff(item, null)
        `when`(manager.rollStaffAccuracy(access, target, null, 2.0)).thenReturn(accurate)
        `when`(manager.rollStaffMaxHit(access, target, 35, 1.0)).thenReturn(23)
        val damage = if (accurate) 23 else 0
        val hit = mock(Hit::class.java)
        `when`(hit.damage).thenReturn(damage)
        `when`(manager.queueMagicHit(access, target, damage, 60, 2)).thenReturn(hit)
        return Fixture(player, access, manager, target, attack, queues, registry[item] as SpecialAttack.Magic)
    }
    private fun complete(block: suspend () -> Boolean): Boolean {
        var value: Boolean? = null
        block.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { value = result.getOrThrow() }
        })
        return requireNotNull(value)
    }
    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
