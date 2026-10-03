package org.rsmod.content.other.special.weapons

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.config.refs.params
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.BlowpipeCharges
import org.rsmod.api.player.worn.BlowpipeCharges.Contents
import org.rsmod.api.random.GameRandom
import org.rsmod.api.weapons.*
import org.rsmod.content.other.special.weapons.ranged.BlowpipeWeapons
import org.rsmod.content.other.special.weapons.scripts.charge.BlowpipeCharging
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
@OptIn(InternalApi::class)
class BlowpipeWeaponsTest {
    @Test fun `all native packed fields round trip without overlap across variants and dart types`() {
        for ((_, empty) in BlowpipeCharges.variants) for (dart in 1..BlowpipeCharges.darts.size) {
            val contents = Contents(dart, BlowpipeCharges.MAX, BlowpipeCharges.MAX)
            val item = BlowpipeCharges.write(InvObj(empty), contents)
            assertEquals(contents, BlowpipeCharges.read(item.copy()))
            assertEquals(BlowpipeCharges.darts[dart - 1].asRSCM(), contents.ammunition!!.id)
            assertEquals(contents.copy(count = BlowpipeCharges.MAX - 1, scales = BlowpipeCharges.MAX - 1), BlowpipeCharges.read(BlowpipeCharges.consume(item, false, true)))
            assertEquals(contents, BlowpipeCharges.read(BlowpipeCharges.consume(item, true, false)))
        }
    }

    @Test fun `loading rejects mixed darts and unload keeps scales while uncharge refunds both`() {
        for ((_, empty) in BlowpipeCharges.variants) {
            val player = player()
            player.inv[0] = InvObj(empty)
            player.inv[1] = InvObj("obj.dragon_dart", 100)
            player.inv[2] = InvObj("obj.snakeboss_scale", 200)
            assertTrue(BlowpipeCharging.load(player, player.inv, 0, 1))
            assertTrue(BlowpipeCharging.load(player, player.inv, 0, 2))
            assertTrue(BlowpipeCharges.read(player.inv[0]).ready)
            player.inv[1] = InvObj("obj.rune_dart", 5)
            assertFalse(BlowpipeCharging.load(player, player.inv, 0, 1))
            assertEquals(5, player.inv[1]!!.count)
            assertTrue(BlowpipeCharging.unload(player, player.inv, 0, false))
            assertEquals(Contents(0, 0, 200), BlowpipeCharges.read(player.inv[0]))
            assertEquals(100, player.inv.objs.filterNotNull().first { it.id == "obj.dragon_dart".asRSCM() }.count)
            assertTrue(BlowpipeCharging.unload(player, player.inv, 0, true))
            assertEquals(empty.asRSCM(), player.inv[0]!!.id)
            assertEquals(200, player.inv.objs.filterNotNull().first { it.id == "obj.snakeboss_scale".asRSCM() }.count)
        }
    }

    @Test fun `failed unload cannot delete resources and loading capacity consumes only accepted amount`() {
        val player = player()
        val item = BlowpipeCharges.write(InvObj(BlowpipeCharges.variants.first().first), Contents(1, BlowpipeCharges.MAX - 2, 5))
        player.inv[0] = item
        player.inv[1] = InvObj(BlowpipeCharges.darts.first(), 10)
        assertTrue(BlowpipeCharging.load(player, player.inv, 0, 1))
        assertEquals(8, player.inv[1]!!.count)
        for (slot in 1..27) player.inv[slot] = InvObj("obj.dragon_dagger")
        val full = player.inv[0]!!
        assertFalse(BlowpipeCharging.unload(player, player.inv, 0, true))
        assertSame(full, player.inv[0])
    }

    @Test fun `ranged formula includes loaded dart bonus and ignores unrelated quiver arrows`() {
        val player = player()
        val loaded = BlowpipeCharges.variants.first().first
        for (dart in 1..BlowpipeCharges.darts.size) {
            val item = BlowpipeCharges.write(InvObj(loaded), Contents(dart, 10, 10))
            player.worn[Wearpos.RightHand.slot] = item
            val expected = getInvObj(item).param(params.ranged_strength) + getInvObj(InvObj(BlowpipeCharges.darts[dart - 1])).param(params.ranged_strength)
            player.worn[Wearpos.Quiver.slot] = InvObj("obj.dragon_arrow", 1000)
            assertEquals(expected, WornBonuses().rangedStrengthBonus(player))
        }
    }

    @Test fun `last dart launches once with loaded bonus before consumption and rapid delay`() {
        val player = player()
        val item = BlowpipeCharges.write(InvObj(BlowpipeCharges.variants.first().first), Contents(8, 1, 1))
        player.worn[Wearpos.RightHand.slot] = item
        val access = mock(ProtectedAccess::class.java)
        val random = mock(GameRandom::class.java)
        `when`(access.player).thenReturn(player)
        `when`(access.random).thenReturn(random)
        `when`(random.of(3)).thenReturn(1)
        val manager = mock(WeaponAttackManager::class.java)
        val registry = WeaponRegistry()
        with(BlowpipeWeapons()) { WeaponRepository(registry).register(manager) }
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
        val attack = CombatAttack.Ranged(item, RangedAttackType.Light, RangedAttackStyle.Rapid)
        val dart = getInvObj(InvObj("obj.dragon_dart"))
        val travel = RSCM.getReverseMapping(RSCMType.SPOTANIM, dart.param(params.proj_travel).id)
        val proj = mock(ProjAnim::class.java)
        `when`(proj.clientCycles).thenReturn(25)
        `when`(proj.serverCycles).thenReturn(1)
        `when`(manager.spawnProjectile(access, target, travel, "projanim.thrown")).thenReturn(proj)
        `when`(manager.rollRangedDamage(access, target, attack)).thenAnswer {
            assertTrue(BlowpipeCharges.read(player.worn[Wearpos.RightHand.slot]).ready)
            10
        }
        complete { with(registry.getRanged(item)!!) { access.attack(target, attack) } }
        verify(manager).setNextAttackDelay(access, 2)
        verify(manager).queueRangedHit(access, target, dart, 10, 25, 1)
        assertEquals(Contents(0, 0, 0), BlowpipeCharges.read(player.worn[Wearpos.RightHand.slot]))
        verify(manager).stopCombat(access)
    }

    @Test fun `menus register both ornaments and supported dart resources`() {
        with(BlowpipeCharging()) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
    }

    private fun player() = Player().apply {
        inv = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())), arrayOfNulls(28))
        worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
    }
    private fun complete(block: suspend () -> Boolean) {
        var done = false
        block.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { assertTrue(result.getOrThrow()); done = true }
        })
        assertTrue(done)
    }
    companion object {
        @JvmStatic @BeforeAll fun setup() {
            ServerCacheManager.init(240).close()
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) { ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup() }
        }
    }
}
