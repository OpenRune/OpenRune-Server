package org.rsmod.content.other.special.weapons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.refs.params
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.AyakCharges
import org.rsmod.api.weapons.*
import org.rsmod.content.other.special.weapons.magic.AyakWeapons
import org.rsmod.content.other.special.weapons.scripts.charge.AyakCharging
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.game.type.getInvObj
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class AyakWeaponsTest {
    @Test fun `cache routes both identities to powered staff and every item handler binds`() {
        val registry = registry(mock(WeaponAttackManager::class.java))
        for (symbol in listOf(AyakCharges.CHARGED, AyakCharges.EMPTY)) {
            val item = InvObj(symbol)
            assertEquals("PoweredStaff", getInvObj(item).weaponCategory.name)
            assertEquals(6, getInvObj(item).param(params.attackrange))
            assertEquals(3, getInvObj(item).param(params.attackrate))
            Assertions.assertNotNull(registry.getMagic(item))
        }
        with(AyakCharging()) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
    }

    @Test fun `fifty thousand charges and material identity survive item copies`() {
        for (source in AyakCharges.Source.entries) {
            val item = AyakCharges.write(InvObj(AyakCharges.EMPTY, vars = 1 shl 25), 50_000, source).copy()
            assertEquals(50_000, AyakCharges.count(item))
            assertEquals(source, AyakCharges.source(item))
            assertTrue(item.vars and (1 shl 25) != 0)
            assertEquals(1, AyakCharges.count(AyakCharges.write(item, 1)))
            assertEquals(AyakCharges.EMPTY.asRSCM(), AyakCharges.write(item, 0).id)
            assertThrows(IllegalArgumentException::class.java) { AyakCharges.write(item, 50_001) }
        }
    }

    @Test fun `both charge recipes refund exactly and cannot be mixed`() {
        for (source in AyakCharges.Source.entries) {
            val player = player()
            player.inv[0] = InvObj(AyakCharges.EMPTY)
            source.recipe.forEachIndexed { index, (symbol, cost) -> player.inv[index + 1] = InvObj(symbol, cost * 5) }
            assertTrue(AyakCharging.transfer(player, player.inv, 0, player.inv[0]!!, 5, source, false))
            val charged = player.inv[0]!!
            assertEquals(5, AyakCharges.count(charged))
            assertFalse(AyakCharging.transfer(player, player.inv, 0, charged, 1, AyakCharges.Source.entries.first { it != source }, false))
            assertSame(charged, player.inv[0])
            assertTrue(AyakCharging.transfer(player, player.inv, 0, charged, 5, source, true))
            source.recipe.forEach { (symbol, cost) -> assertEquals(cost * 5, player.inv.objs.filterNotNull().filter { it.id == symbol.asRSCM() }.sumOf { it.count }) }
        }
    }

    @Test fun `insufficient resources stale item and full refund inventory remain unchanged`() {
        val player = player()
        val empty = InvObj(AyakCharges.EMPTY)
        player.inv[0] = empty
        player.inv[1] = InvObj("obj.deathrune", 20)
        assertFalse(AyakCharging.transfer(player, player.inv, 0, empty, 1, AyakCharges.Source.Runes, false))
        assertSame(empty, player.inv[0])
        assertEquals(20, player.inv[1]!!.count)
        val charged = AyakCharges.write(empty, 10)
        player.inv[0] = charged
        for (slot in 1..27) player.inv[slot] = InvObj("obj.dragon_dagger")
        assertFalse(AyakCharging.transfer(player, player.inv, 0, charged, 10, AyakCharges.Source.Runes, true))
        assertFalse(AyakCharging.transfer(player, player.inv, 0, empty, 1, AyakCharges.Source.Runes, false))
        assertSame(charged, player.inv[0])
        assertEquals(0, player.inv.freeSpace())
    }

    @Test fun `last charge uses source cast target impact and projectile arrival even on splash`() {
        for (accurate in listOf(false, true)) {
            val player = player()
            player.statMap.setCurrentLevel("stat.magic", 99.toByte())
            val weapon = AyakCharges.write(InvObj(AyakCharges.EMPTY), 1)
            player.worn[Wearpos.RightHand.slot] = weapon
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(player)
            val manager = mock(WeaponAttackManager::class.java)
            val target = spy(Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }))
            val attack = CombatAttack.Staff(weapon, null)
            val projectile = mock(ProjAnim::class.java)
            `when`(projectile.clientCycles).thenReturn(64)
            `when`(projectile.serverCycles).thenReturn(3)
            `when`(manager.spawnProjectile(access, target, "spotanim.vfx_ayak_normal_projectile", "projanim.magic_spell")).thenReturn(projectile)
            `when`(manager.rollStaffAccuracy(access, target, attack)).thenReturn(accurate)
            `when`(manager.rollStaffMaxHit(access, target, 27)).thenReturn(19)
            val handler = registry(manager).getMagic(weapon)!!
            complete { with(handler) { access.attack(target, attack) } }
            verify(access).spotanim("spotanim.vfx_ayak_player_normal_spotanim", 0, 0, 0)
            verify(manager).setNextAttackDelay(access, 3)
            if (accurate) {
                verify(target).spotanim("spotanim.vfx_ayak_normal_impact", 64, 0, 0)
                verify(manager).queueMagicHit(access, target, 19, 64, 3, null)
            } else verify(manager).queueSplashHit(access, target, 64, 3, null)
            assertEquals(AyakCharges.EMPTY.asRSCM(), player.worn[Wearpos.RightHand.slot]!!.id)
            verify(manager).stopCombat(access)
        }
    }

    @Test fun `unfunded spawn and player target cannot cast for free`() {
        val player = player()
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(player)
        val manager = mock(WeaponAttackManager::class.java)
        val weapon = InvObj(AyakCharges.CHARGED)
        player.worn[Wearpos.RightHand.slot] = weapon
        val handler = registry(manager).getMagic(weapon)!!
        complete { with(handler) { access.attack(Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }), CombatAttack.Staff(weapon, null)) } }
        verify(manager).stopCombat(access)
        verifyNoMoreInteractions(manager)
        clearInvocations(manager)
        val charged = AyakCharges.write(weapon, 10)
        player.worn[Wearpos.RightHand.slot] = charged
        complete { with(handler) { access.attack(Player(), CombatAttack.Staff(charged, null)) } }
        assertSame(charged, player.worn[Wearpos.RightHand.slot])
        verify(manager).stopCombat(access)
        verifyNoMoreInteractions(manager)
        assertEquals(27, AyakCharges.baseMaxHit(99))
        assertEquals(32, AyakCharges.baseMaxHit(114))
    }
    private fun registry(manager: WeaponAttackManager) = WeaponRegistry().also {
        with(AyakWeapons()) { WeaponRepository(it).register(manager) }
    }
    private fun player() = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
    }
    private fun complete(block: suspend () -> Boolean) {
        var complete = false
        block.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { assertTrue(result.getOrThrow()); complete = true }
        })
        assertTrue(complete)
    }
    companion object {
        @JvmStatic @BeforeAll fun setup() {
            ServerCacheManager.init(240).close()
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
        }
    }
}
