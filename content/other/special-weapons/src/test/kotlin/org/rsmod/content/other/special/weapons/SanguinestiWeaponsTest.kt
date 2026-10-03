package org.rsmod.content.other.special.weapons

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
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.weapons.*
import org.rsmod.content.other.special.weapons.magic.SanguinestiWeapons
import org.rsmod.content.other.special.weapons.scripts.charge.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.*
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class SanguinestiWeaponsTest {
    @Test fun `ordinary and holy identities retain twenty thousand charges and bind menus`() {
        val registry = registry(mock(WeaponAttackManager::class.java))
        for ((charged, empty) in SanguinestiCharges.variants) {
            for (symbol in listOf(charged, empty)) Assertions.assertNotNull(registry.getMagic(InvObj(symbol)))
            val item = SanguinestiCharges.write(InvObj(empty, vars = 1 shl 25), 20_000).copy()
            assertEquals(charged.asRSCM(), item.id)
            assertEquals(20_000, SanguinestiCharges.count(item))
            assertTrue(item.vars and (1 shl 25) != 0)
            assertEquals(empty.asRSCM(), SanguinestiCharges.write(item, 0).id)
            assertThrows(IllegalArgumentException::class.java) { SanguinestiCharges.write(item, 20_001) }
        }
        with(SanguinestiCharging()) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
    }

    @Test fun `blood payments and refunds are atomic and preserve ornament`() {
        for ((charged, empty) in SanguinestiCharges.variants) {
            val p = player()
            p.inv[0] = InvObj(empty)
            p.inv[1] = InvObj("obj.bloodrune", 40000)
            assertTrue(SanguinestiCharging.transfer(p, p.inv, 0, p.inv[0]!!, 20000, false))
            assertEquals(charged.asRSCM(), p.inv[0]!!.id)
            assertEquals(27, p.inv.freeSpace())
            val original = p.inv[0]!!
            assertFalse(SanguinestiCharging.transfer(p, p.inv, 0, original, 1, false))
            for (slot in 1..27) p.inv[slot] = InvObj("obj.dragon_dagger")
            assertFalse(SanguinestiCharging.transfer(p, p.inv, 0, original, 20000, true))
            assertSame(original, p.inv[0])
            p.inv[1] = null
            assertTrue(SanguinestiCharging.transfer(p, p.inv, 0, original, 20000, true))
            assertEquals(empty.asRSCM(), p.inv[0]!!.id)
            assertEquals(40000, p.inv[1]!!.count)
            assertFalse(SanguinestiCharging.transfer(p, p.inv, 0, original, 1, false))
        }
    }

    @Test fun `leech adds eight damage but heals half actual impact and preserves holy effects`() {
        for ((charged, _) in SanguinestiCharges.variants) {
            val f = fixture(charged, leech = true)
            assertTrue(complete(f.action))
            verify(f.manager).queueMagicHit(f.access, f.target, 29, 60, 3, null)
            assertEquals(50.toByte(), f.player.statMap.getCurrentLevel("stat.hitpoints"))
            f.effects.complete(9)
            assertEquals(54.toByte(), f.player.statMap.getCurrentLevel("stat.hitpoints"))
            f.effects.complete(29)
            assertEquals(54.toByte(), f.player.statMap.getCurrentLevel("stat.hitpoints"))
            val suffix = if (charged.endsWith("_or")) "_justiciar" else ""
            verify(f.access).spotanim("spotanim.sanguinesti_staff_casting$suffix", 0, 100, 0)
            verify(f.target).spotanim("spotanim.sanguinesti_staff_impact$suffix", 60, 100, 0)
            verify(f.player).spotanim("spotanim.sanguinesti_staff_heal$suffix", 0, 100, 0)
            assertEquals(1, SanguinestiCharges.count(f.player.worn[Wearpos.RightHand.slot]!!))
        }
    }

    @Test fun `ordinary successful hit has no leech and zero or replaced-login impact never heals`() {
        val normal = fixture(leech = false)
        complete(normal.action)
        verify(normal.manager).queueMagicHit(normal.access, normal.target, 21, 60, 3, null)
        normal.effects.complete(21)
        assertEquals(50.toByte(), normal.player.statMap.getCurrentLevel("stat.hitpoints"))
        val zero = fixture(leech = true)
        complete(zero.action)
        zero.effects.complete(0)
        assertEquals(50.toByte(), zero.player.statMap.getCurrentLevel("stat.hitpoints"))
        val relog = fixture(leech = true)
        complete(relog.action)
        relog.player.uuid = 2
        relog.player.assignUid()
        relog.effects.complete(29)
        assertEquals(50.toByte(), relog.player.statMap.getCurrentLevel("stat.hitpoints"))
    }

    @Test fun `last-charge splash consumes exactly one and stops without a leech roll`() {
        val f = fixture(charges = 1, accurate = false)
        complete(f.action)
        assertEquals(0, SanguinestiCharges.count(f.player.worn[Wearpos.RightHand.slot]!!))
        verify(f.manager).queueSplashHit(f.access, f.target, 60, 3, null)
        verify(f.manager).stopCombat(f.access)
        verifyNoInteractions(f.random)
    }

    @Test fun `empty and pvp casts neither attack nor spend charges`() {
        val empty = fixture(charges = 0)
        complete(empty.action)
        verify(empty.manager).stopCombat(empty.access)
        verifyNoMoreInteractions(empty.manager)
        val f = fixture()
        val handler = registry(f.manager).getMagic(f.attack.weapon)!!
        complete { with(handler) { f.access.attack(Player(), f.attack) } }
        assertEquals(2, SanguinestiCharges.count(f.player.worn[Wearpos.RightHand.slot]!!))
        verify(f.manager).stopCombat(f.access)
        verifyNoMoreInteractions(f.manager)
    }
    private data class Fixture(val player: Player, val access: ProtectedAccess, val manager: WeaponAttackManager, val target: Npc, val attack: CombatAttack.Staff, val random: GameRandom, val effects: HitImpactEffects, val action: suspend () -> Boolean)
    private fun fixture(symbol: String = "obj.sanguinesti_staff", charges: Int = 2, accurate: Boolean = true, leech: Boolean = true): Fixture {
        val p = spy(player()).apply {
            slotId = 1; uuid = 1; assignUid()
            statMap.setCurrentLevel("stat.magic", 99.toByte())
            statMap.setBaseLevel("stat.hitpoints", 99.toByte())
            statMap.setCurrentLevel("stat.hitpoints", 50.toByte())
        }
        val item = if (charges == 0) InvObj(symbol) else SanguinestiCharges.write(InvObj(symbol), charges)
        p.worn[Wearpos.RightHand.slot] = item
        val access = mock(ProtectedAccess::class.java)
        val random = mock(GameRandom::class.java)
        `when`(access.player).thenReturn(p)
        `when`(access.random).thenReturn(random)
        `when`(random.of(5)).thenReturn(if (leech) 0 else 1)
        val manager = mock(WeaponAttackManager::class.java)
        val target = spy(Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }))
        val attack = CombatAttack.Staff(item, null)
        val proj = mock(ProjAnim::class.java)
        `when`(proj.clientCycles).thenReturn(60)
        `when`(proj.serverCycles).thenReturn(3)
        val suffix = if (symbol.endsWith("_or")) "_justiciar" else ""
        `when`(manager.spawnProjectile(access, target, "spotanim.sanguinesti_staff_travel$suffix", "projanim.magic_spell")).thenReturn(proj)
        `when`(manager.rollStaffAccuracy(access, target, attack)).thenReturn(accurate)
        `when`(manager.rollStaffMaxHit(access, target, 33)).thenReturn(21)
        val effects = HitImpactEffects()
        val hit = Hit(HitType.Magic, Hitmark(0).copy(damage = if (leech) 29 else 21), null, null, null, effects)
        `when`(manager.queueMagicHit(access, target, if (leech) 29 else 21, 60, 3)).thenReturn(hit)
        val handler = registry(manager).getMagic(item)!!
        return Fixture(p, access, manager, target, attack, random, effects) { with(handler) { access.attack(target, attack) } }
    }
    private fun registry(manager: WeaponAttackManager) = WeaponRegistry().also { with(SanguinestiWeapons()) { WeaponRepository(it).register(manager) } }
    private fun player() = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
    }
    private fun complete(block: suspend () -> Boolean): Boolean {
        var value: Boolean? = null
        block.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { value = result.getOrThrow() }
        })
        return requireNotNull(value)
    }
    companion object {
        @JvmStatic @BeforeAll fun setup() {
            ServerCacheManager.init(240).close()
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
        }
    }
}
