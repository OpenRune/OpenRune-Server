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
import org.rsmod.api.weapons.*
import org.rsmod.content.other.special.weapons.magic.TridentWeapons
import org.rsmod.content.other.special.weapons.scripts.charge.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class TridentWeaponsTest {
    @Test fun `all cache variants and native item menus register without duplicates`() {
        val registry = registry(mock(WeaponAttackManager::class.java))
        val context = ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache())
        with(TridentCharging()) { context.startup() }
        for (kind in TridentCharges.kinds) for (symbol in kind.symbols) {
            Assertions.assertNotNull(registry.getMagic(InvObj(symbol)), symbol)
            kind.recipe.forEach { assertTrue(it.first.asRSCM() >= 0) }
        }
    }

    @Test fun `full tradable tridents lose one charge and preserve cosmetic variant`() {
        for (kind in TridentCharges.kinds.filter { it.full != null }) {
            val original = InvObj(kind.full!!)
            assertEquals(2500, TridentCharges.count(original))
            val used = TridentCharges.withCharges(original, 2499)
            assertEquals(kind.charged.asRSCM(), used.id)
            assertEquals(2499, TridentCharges.count(used))
        }
    }

    @Test fun `charges survive item copies and stay separate between identical weapons`() {
        for (kind in TridentCharges.kinds) {
            val first = TridentCharges.withCharges(InvObj(kind.empty), kind.max - 1)
            val second = TridentCharges.withCharges(InvObj(kind.empty), 1)
            assertEquals(kind.max - 1, TridentCharges.count(first.copy()))
            assertEquals(1, TridentCharges.count(second))
            assertEquals(kind.empty.asRSCM(), TridentCharges.withCharges(second, 0).id)
            assertThrows(IllegalArgumentException::class.java) { TridentCharges.withCharges(first, kind.max + 1) }
        }
    }

    @Test fun `charging and refunding are atomic and coins are not refunded`() {
        for (kind in TridentCharges.kinds) {
            val player = player()
            player.inv[0] = InvObj(kind.empty)
            for ((index, resource) in kind.recipe.withIndex()) player.inv[index + 1] = InvObj(resource.first, resource.second * 3)
            assertTrue(TridentCharging.transfer(player, player.inv, 0, player.inv[0]!!, 3, false))
            assertEquals(3, TridentCharges.count(player.inv[0]!!))
            assertEquals(27, player.inv.freeSpace())
            assertTrue(TridentCharging.transfer(player, player.inv, 0, player.inv[0]!!, 3, true))
            assertEquals(kind.empty.asRSCM(), player.inv[0]!!.id)
            assertFalse(player.inv.objs.filterNotNull().any { it.id == "obj.coins".asRSCM() })
            for ((symbol, cost) in kind.refund) assertEquals(cost * 3, player.inv.objs.filterNotNull().filter { it.id == symbol.asRSCM() }.sumOf { it.count })
        }
    }

    @Test fun `missing resources stale selection and full inventory never lose charges or items`() {
        val player = player()
        val kind = TridentCharges.kinds.first()
        val empty = InvObj(kind.empty)
        player.inv[0] = empty
        player.inv[1] = InvObj("obj.deathrune", 10)
        assertFalse(TridentCharging.transfer(player, player.inv, 0, empty, 1, false))
        assertSame(empty, player.inv[0])
        assertEquals(10, player.inv[1]!!.count)
        val charged = TridentCharges.withCharges(empty, 2)
        player.inv[0] = charged
        for (slot in 1..27) player.inv[slot] = InvObj("obj.dragon_dagger")
        assertFalse(TridentCharging.transfer(player, player.inv, 0, charged, 2, true))
        assertSame(charged, player.inv[0])
        assertFalse(TridentCharging.transfer(player, player.inv, 0, empty, 1, false))
        assertEquals(0, player.inv.freeSpace())
    }

    @Test fun `last charge launches one projectile consumes on splash and stops`() {
        for (kind in TridentCharges.kinds) {
            val player = player()
            val weapon = TridentCharges.withCharges(InvObj(kind.empty), 1)
            player.worn[Wearpos.RightHand.slot] = weapon
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(player)
            val manager = mock(WeaponAttackManager::class.java)
            val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
            val projectile = mock(ProjAnim::class.java)
            `when`(projectile.clientCycles).thenReturn(47)
            `when`(projectile.serverCycles).thenReturn(2)
            val prefix = if (kind.toxic) "toxic_tots" else "slayer_tots"
            val suffix = if (kind.charged.endsWith("_orn")) "_orn_leagues6" else ""
            `when`(manager.spawnProjectile(access, target, "spotanim.${prefix}_projectile$suffix", "projanim.magic_spell")).thenReturn(projectile)
            val attack = CombatAttack.Staff(weapon, null)
            val handler = registry(manager).getMagic(weapon)!!
            run { with(handler) { access.attack(target, attack) } }
            assertEquals(kind.empty.asRSCM(), player.worn[Wearpos.RightHand.slot]!!.id)
            verify(manager).queueSplashHit(access, target, 47, 2, null)
            verify(manager).stopCombat(access)
            verify(manager).setNextAttackDelay(access, 4)
        }
    }

    @Test fun `empty trident and player targets cannot produce a free attack`() {
        for (kind in TridentCharges.kinds) {
            val player = player()
            val weapon = InvObj(kind.charged)
            player.worn[Wearpos.RightHand.slot] = weapon
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(player)
            val manager = mock(WeaponAttackManager::class.java)
            val handler = registry(manager).getMagic(weapon)!!
            val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
            run { with(handler) { access.attack(target, CombatAttack.Staff(weapon, null)) } }
            assertEquals(kind.empty.asRSCM(), player.worn[Wearpos.RightHand.slot]!!.id)
            assertEquals(listOf("stopCombat"), mockingDetails(manager).invocations.map { it.method.name })
            clearInvocations(manager)
            val loaded = TridentCharges.withCharges(weapon, 5)
            player.worn[Wearpos.RightHand.slot] = loaded
            run { with(handler) { access.attack(Player(), CombatAttack.Staff(loaded, null)) } }
            assertEquals(5, TridentCharges.count(player.worn[Wearpos.RightHand.slot]!!))
            assertEquals(listOf("stopCombat"), mockingDetails(manager).invocations.map { it.method.name })
        }
    }

    @Test fun `cast effect stays on source and impact follows projectile to target`() {
        for (kind in TridentCharges.kinds) {
            val player = player()
            player.statMap.setCurrentLevel("stat.magic", 99.toByte())
            val weapon = TridentCharges.withCharges(InvObj(kind.empty), 3)
            player.worn[Wearpos.RightHand.slot] = weapon
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(player)
            val manager = mock(WeaponAttackManager::class.java)
            val target = spy(Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }))
            val attack = CombatAttack.Staff(weapon, null)
            val projectile = mock(ProjAnim::class.java)
            `when`(projectile.clientCycles).thenReturn(61)
            `when`(projectile.serverCycles).thenReturn(3)
            val prefix = if (kind.toxic) "toxic_tots" else "slayer_tots"
            val suffix = if (kind.charged.endsWith("_orn")) "_orn_leagues6" else ""
            `when`(manager.spawnProjectile(access, target, "spotanim.${prefix}_projectile$suffix", "projanim.magic_spell")).thenReturn(projectile)
            `when`(manager.rollStaffAccuracy(access, target, attack)).thenReturn(true)
            `when`(manager.rollStaffMaxHit(access, target, if (kind.toxic) 31 else 28)).thenReturn(17)
            val handler = registry(manager).getMagic(weapon)!!
            run { with(handler) { access.attack(target, attack) } }
            verify(access).spotanim("spotanim.${prefix}_casting$suffix", 0, 100, 0)
            verify(target).spotanim("spotanim.${prefix}_impact$suffix", 61, 100, 0)
            verify(manager).queueMagicHit(access, target, 17, 61, 3, null)
            verify(manager).giveCombatXp(access, target, attack, 17)
            assertEquals(2, TridentCharges.count(player.worn[Wearpos.RightHand.slot]!!))
        }
    }

    @Test fun `boosted magic maximum uses the trident family formula`() {
        assertEquals(28, TridentWeapons.baseMaxHit(99, false))
        assertEquals(31, TridentWeapons.baseMaxHit(99, true))
        assertEquals(36, TridentWeapons.baseMaxHit(114, true))
    }

    private fun registry(manager: WeaponAttackManager) = WeaponRegistry().also {
        with(TridentWeapons()) { WeaponRepository(it).register(manager) }
    }
    private fun player() = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
    }
    private fun run(block: suspend () -> Boolean) {
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
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
    }
}
