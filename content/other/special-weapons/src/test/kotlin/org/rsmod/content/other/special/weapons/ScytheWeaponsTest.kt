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
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.repo.world.WorldRepository
import org.rsmod.api.weapons.*
import org.rsmod.content.other.special.weapons.melee.ScytheOfViturWeapons
import org.rsmod.content.other.special.weapons.scripts.charge.*
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class ScytheWeaponsTest {
    @Test fun `all normal holy and sanguine charging menu bindings exist`() {
        val context = ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache())
        with(ScytheCharging()) { context.startup() }
        val manager = mock(WeaponAttackManager::class.java)
        val registry = WeaponRegistry()
        with(ScytheOfViturWeapons(mock(WorldRepository::class.java))) { WeaponRepository(registry).register(manager) }
        for ((charged, empty) in ScytheCharges.variants) {
            Assertions.assertNotNull(registry.getMelee(InvObj(charged)))
            Assertions.assertNotNull(registry.getMelee(InvObj(empty)))
        }
    }

    @Test fun `recharge pays 200 blood runes and one vial for 100 persistent charges`() {
        for ((charged, empty) in ScytheCharges.variants) {
            val player = player()
            player.inv[0] = InvObj(empty)
            player.inv[1] = InvObj("obj.bloodrune", 401)
            for (slot in 2..4) player.inv[slot] = InvObj("obj.vial_blood")
            assertTrue(ScytheCharging.recharge(player, player.inv, 0, player.inv[0]!!, 2))
            assertEquals(charged.asRSCM(), player.inv[0]!!.id)
            assertEquals(200, ScytheCharges.count(player.inv[0]!!.copy()))
            assertEquals(1, player.inv[1]!!.count)
            assertEquals(1, player.inv.objs.filterNotNull().count { it.id == "obj.vial_blood".asRSCM() })
            val original = player.inv[0]!!
            assertFalse(ScytheCharging.recharge(player, player.inv, 0, original, 1))
            assertSame(original, player.inv[0])
            assertEquals(1, player.inv[1]!!.count)
        }
    }

    @Test fun `charge limit and integer bounds reject excess without consuming resources`() {
        assertEquals(200, ScytheCharges.batches(0, Int.MAX_VALUE, Int.MAX_VALUE))
        assertEquals(0, ScytheCharges.batches(19901, 200, 1))
        assertEquals(1, ScytheCharges.batches(19900, 200, 1))
        val player = player()
        val weapon = ScytheCharges.withCharges(InvObj(ScytheCharges.variants.first().first), 20000)
        player.inv[0] = weapon
        assertFalse(ScytheCharging.recharge(player, player.inv, 0, weapon, Int.MAX_VALUE))
        assertSame(weapon, player.inv[0])
    }

    @Test fun `three hits spend exactly one charge after the last hit and keep ornament`() {
        for ((charged, empty) in ScytheCharges.variants) {
            val f = fixture(charged, 1, 7)
            complete(f.action)
            assertEquals(empty.asRSCM(), f.player.worn[Wearpos.RightHand.slot]!!.id)
            assertEquals(3, mockingDetails(f.manager).invocations.count { it.method.name == "queueMeleeHit" })
            val rolls = mockingDetails(f.manager).invocations.filter { it.method.name == "rollMeleeDamage" }
            assertEquals(listOf(1.0, 0.5, 0.25), rolls.map { it.arguments[4] })
            verify(f.manager).giveCombatXp(f.access, f.target, f.attack, 21)
        }
    }

    @Test fun `all misses preserve charge and zero charge spawns use empty item before damage`() {
        val missed = fixture(ScytheCharges.variants.first().first, 2, 0)
        complete(missed.action)
        assertEquals(2, ScytheCharges.count(missed.player.worn[Wearpos.RightHand.slot]!!))
        val empty = fixture(ScytheCharges.variants.first().first, 0, 4)
        complete(empty.action)
        val expected = ScytheCharges.variants.first().second.asRSCM()
        assertEquals(expected, empty.player.worn[Wearpos.RightHand.slot]!!.id)
        assertTrue(mockingDetails(empty.manager).invocations.filter { it.method.name == "rollMeleeDamage" }.all { (it.arguments[2] as CombatAttack.Melee).weapon!!.id == expected })
    }

    private data class Fixture(val player: Player, val access: ProtectedAccess, val manager: WeaponAttackManager, val target: Npc, val attack: CombatAttack.Melee, val action: suspend () -> Boolean)
    private fun fixture(symbol: String, count: Int, damage: Int): Fixture {
        val player = player()
        player.coords = CoordGrid(3200, 3200, 0)
        val item = if (count == 0) InvObj(symbol) else ScytheCharges.withCharges(InvObj(symbol), count)
        player.worn[Wearpos.RightHand.slot] = item
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(player)
        val manager = mock(WeaponAttackManager::class.java)
        val registry = WeaponRegistry()
        with(ScytheOfViturWeapons(mock(WorldRepository::class.java))) { WeaponRepository(registry).register(manager) }
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.size >= 3 })
        target.coords = CoordGrid(3200, 3201, 0)
        val attack = CombatAttack.Melee(item, MeleeAttackType.Slash, MeleeAttackStyle.Aggressive, CombatStance.Stance1)
        val normalized = attack.copy(weapon = ScytheCharges.withCharges(item, count))
        for (multiplier in listOf(1.0, 0.5, 0.25)) {
            `when`(manager.rollMeleeDamage(access, target, normalized, 1.0, multiplier)).thenReturn(damage)
        }
        val handler = registry.getMelee(item)!!
        return Fixture(player, access, manager, target, normalized) { with(handler) { access.attack(target, attack) } }
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
            with(InvTransactionsScript(mock(PlayerItemStorage::class.java))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }
    }
}
