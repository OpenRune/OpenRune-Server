package org.rsmod.content.other.special.attacks.shield

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.util.Wearpos
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.DragonfireProtection
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.events.interact.HeldUEvents
import org.rsmod.api.player.events.interact.WornObjEvents
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.DragonfireShields
import org.rsmod.api.player.worn.DragonfireShields.Kind
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.SpecialAttackType
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
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
class DragonfireShieldTest {
    @Test fun `equipped menu operations inspect toggle and recharge both wyvern variants`() {
        val fixture = menu()
        fixture.player.worn[Wearpos.LeftHand.slot] = InvObj(Kind.WYVERN.uncharged)
        fixture.player.inv[0] = InvObj("obj.fossil_numulite", 5_300)
        complete {
            val shield = fixture.player.worn[Wearpos.LeftHand.slot]!!
            assertTrue(fixture.bus.publish(fixture.access, WornObjEvents.Op2(Wearpos.LeftHand.slot, shield)))
            assertTrue(fixture.bus.publish(fixture.access, WornObjEvents.Op3(Wearpos.LeftHand.slot, shield)))
        }
        assertEquals(10, DragonfireShields.charges(fixture.player.worn[Wearpos.LeftHand.slot]))
        assertEquals(300, fixture.player.inv[0]!!.count)
        complete {
            val shield = fixture.player.worn[Wearpos.LeftHand.slot]!!
            assertTrue(fixture.bus.publish(fixture.access, WornObjEvents.Op2(Wearpos.LeftHand.slot, shield)))
        }
        assertEquals(SpecialAttackType.Shield, fixture.player.shieldSpecialType)
        complete {
            val shield = fixture.player.worn[Wearpos.LeftHand.slot]!!
            assertTrue(fixture.bus.publish(fixture.access, WornObjEvents.Op2(Wearpos.LeftHand.slot, shield)))
        }
        assertEquals(SpecialAttackType.None, fixture.player.shieldSpecialType)
    }

    @Test fun `bottled breath fills either fire shield once and does not consume at full charge`() {
        for (kind in listOf(Kind.SHIELD, Kind.WARD)) {
            val fixture = menu()
            fixture.player.inv[0] = InvObj("obj.bottled_dragonbreath", 2)
            fixture.player.inv[1] = InvObj(kind.uncharged)
            fun use() = complete {
                val bottle = fixture.player.inv[0]!!
                val shield = fixture.player.inv[1]!!
                assertTrue(fixture.bus.publish(fixture.access, HeldUEvents.Type(getInvObj(bottle), 0, getInvObj(shield), 1)))
            }
            use()
            assertEquals(50, DragonfireShields.charges(fixture.player.inv[1]))
            assertEquals(1, fixture.player.inv[0]!!.count)
            use()
            assertEquals(1, fixture.player.inv[0]!!.count)
        }
    }

    @Test fun `charges remain on each individual shield and all variants handle empty charged spawns`() {
        for (kind in Kind.entries) {
            val raw = InvObj(kind.charged, vars = 1 shl 20)
            assertEquals(0, DragonfireShields.charges(raw))
            val charged = DragonfireShields.withCharges(raw, 50)
            assertEquals(50, DragonfireShields.charges(charged))
            assertEquals(1 shl 20, charged.vars and (1 shl 20))
            val empty = DragonfireShields.withCharges(charged, 0)
            assertEquals(kind.uncharged.asRSCM(), empty.id)
            assertEquals(0, DragonfireShields.charges(empty))
            assertEquals(50, DragonfireShields.charges(charged))
        }
    }

    @Test fun `charge defence bonuses affect melee and ranged but not magic`() {
        val bonuses = WornBonuses()
        for (kind in Kind.entries) {
            val player = player()
            val shield = InvObj(kind.uncharged)
            player.worn[Wearpos.LeftHand.slot] = shield
            val before = bonuses.calculate(player)
            player.worn[Wearpos.LeftHand.slot] = DragonfireShields.withCharges(shield, 50)
            val after = bonuses.calculate(player)
            assertEquals(before.defStab + 50, after.defStab)
            assertEquals(before.defSlash + 50, after.defSlash)
            assertEquals(before.defCrush + 50, after.defCrush)
            assertEquals(before.defRange + 50, after.defRange)
            assertEquals(before.defMagic, after.defMagic)
        }
    }

    @Test fun `breath charging caps at fifty and never charges the fossil shield`() {
        for (kind in Kind.entries) {
            val player = player()
            player.worn[Wearpos.LeftHand.slot] = InvObj(kind.uncharged)
            repeat(55) { DragonfireProtection.absorb(player) }
            assertEquals(if (kind == Kind.WYVERN) 0 else 50,
                DragonfireShields.charges(player.worn[Wearpos.LeftHand.slot]))
        }
    }

    @Test fun `discharging consumes one charge and enforces a persistent cooldown without weapon energy`() {
        val player = player()
        val manager = mock(SpecialAttackManager::class.java)
        val access = mock(ProtectedAccess::class.java)
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
        `when`(access.player).thenReturn(player)
        `when`(access.random).thenReturn(mock(GameRandom::class.java))
        val projectile = mock(ProjAnim::class.java)
        `when`(projectile.clientCycles).thenReturn(30)
        `when`(projectile.serverCycles).thenReturn(1)
        `when`(manager.spawnProjectile(access, target, "spotanim.qip_dragon_slayer_player_projanim", "projanim.dragonfire")).thenReturn(projectile)
        val registry = SpecialAttackRegistry(SpecialAttackWeapons())
        with(DragonfireShieldSpecialAttacks(WornBonuses())) { SpecialAttackRepository(registry).register(manager) }
        val shield = DragonfireShields.withCharges(InvObj(Kind.SHIELD.uncharged), 1)
        player.worn[Wearpos.LeftHand.slot] = shield
        val special = registry[shield] as SpecialAttack.Shield
        complete { special.attack(access, target) }
        assertEquals(Kind.SHIELD.uncharged.asRSCM(), player.worn[Wearpos.LeftHand.slot]!!.id)
        assertTrue(ShieldCooldown.remaining(player.shieldReadyEpoch) in 119..120)
        verify(manager).queueMagicHit(access, target, 0, 30, 1, null)
        player.worn[Wearpos.LeftHand.slot] = DragonfireShields.withCharges(shield, 50)
        complete { special.attack(access, target) }
        assertEquals(50, DragonfireShields.charges(player.worn[Wearpos.LeftHand.slot]))
        verify(manager, times(1)).spawnProjectile(access, target, "spotanim.qip_dragon_slayer_player_projanim", "projanim.dragonfire")
        assertTrue(mockingDetails(manager).invocations.none { it.method.name == "takeSpecialEnergy" })
    }

    @Test fun `cooldown expires at boundary and future clock anomalies remain bounded`() {
        assertEquals(120, ShieldCooldown.remaining(1_120, 1_000))
        assertEquals(1, ShieldCooldown.remaining(1_120, 1_119))
        assertEquals(0, ShieldCooldown.remaining(1_120, 1_120))
        assertEquals(0, ShieldCooldown.remaining(1_120, 1_200))
        assertEquals(120, ShieldCooldown.remaining(Int.MAX_VALUE, 1_000))
    }

    @Test fun `each shield queues its rolled damage with the actual projectile timing`() {
        for (kind in Kind.entries) {
            val player = player()
            val manager = mock(SpecialAttackManager::class.java)
            val access = mock(ProtectedAccess::class.java)
            val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
            val rng = mock(GameRandom::class.java)
            val damage = if (kind == Kind.WYVERN) 15 else 25
            `when`(rng.of(anyInt())).thenReturn(2, 1, damage)
            `when`(access.player).thenReturn(player)
            `when`(access.random).thenReturn(rng)
            val projectile = mock(ProjAnim::class.java)
            `when`(projectile.clientCycles).thenReturn(60)
            `when`(projectile.serverCycles).thenReturn(2)
            val travel = if (kind == Kind.WYVERN) "spotanim.wyvern_skeleton_travel_breath_ancient" else "spotanim.qip_dragon_slayer_player_projanim"
            `when`(manager.spawnProjectile(access, target, travel, "projanim.dragonfire")).thenReturn(projectile)
            val registry = SpecialAttackRegistry(SpecialAttackWeapons())
            with(DragonfireShieldSpecialAttacks(WornBonuses())) { SpecialAttackRepository(registry).register(manager) }
            val shield = DragonfireShields.withCharges(InvObj(kind.uncharged), 3)
            player.worn[Wearpos.LeftHand.slot] = shield
            complete { (registry[shield] as SpecialAttack.Shield).attack(access, target) }
            verify(manager).queueMagicHit(access, target, damage, 60, 2, null)
            assertEquals(2, DragonfireShields.charges(player.worn[Wearpos.LeftHand.slot]))
            assertTrue(mockingDetails(manager).invocations.none { it.method.name == "takeSpecialEnergy" })
        }
    }

    private fun player() = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
    }

    private data class Menu(val player: Player, val access: ProtectedAccess, val bus: EventBus)

    private fun menu(): Menu {
        val player = player()
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(player)
        val bus = EventBus()
        val scripts = ScriptContext(bus, CheatCommandMap(), EngineQueueCache())
        with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { scripts.startup() }
        with(DragonfireShieldScript()) { scripts.startup() }
        return Menu(player, access, bus)
    }

    private fun complete(block: suspend () -> Unit) {
        var finished = false
        block.startCoroutine(object : Continuation<Unit> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Unit>) { result.getOrThrow(); finished = true }
        })
        assertTrue(finished)
    }

    companion object {
        @JvmStatic @BeforeAll fun loadCache() {
            ServerCacheManager.init(240).close()
            assertNotNull(ServerCacheManager.getVarp("varp.dragonfire_special_ready_epoch".asRSCM()))
        }
    }
}
