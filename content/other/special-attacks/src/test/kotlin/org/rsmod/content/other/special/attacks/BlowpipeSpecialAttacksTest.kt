package org.rsmod.content.other.special.attacks

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
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.BlowpipeCharges
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.content.other.special.attacks.ranged.BlowpipeSpecialAttacks
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class BlowpipeSpecialAttacksTest {
    @Test fun `both cache special variants cost fifty percent and heal source after projectile delay`() {
        for ((symbol, _) in BlowpipeCharges.variants) {
            val f = fixture(symbol, 2)
            assertEquals(500, f.special.energyInHundreds)
            assertTrue(complete(f.action))
            verify(f.manager).rollRangedDamage(f.access, f.target, f.attack, 2.0, 1.5, RangedAttackType.Light, RangedAttackStyle.Rapid, RangedAttackType.Light, 0)
            assertEquals(1, BlowpipeCharges.read(f.player.worn[Wearpos.RightHand.slot]).count)
            verify(f.manager).queueRangedHit(f.access, f.target, getInvObj(InvObj("obj.dragon_dart")), 21, 45, 2)
            assertEquals(50.toByte(), f.player.statMap.getCurrentLevel("stat.hitpoints"))
            val scheduled = f.queues.iterator().next()
            assertEquals(2, scheduled.remainingCycles)
            scheduled.action()
            assertEquals(60.toByte(), f.player.statMap.getCurrentLevel("stat.hitpoints"))
            val fx = if (symbol.endsWith("ornament")) "spotanim.toxic_blowpipe_specialattack_league04" else "spotanim.toxic_blowpipe_specialattack"
            verify(f.access).spotanim(fx, 0, 96, 0)
        }
    }

    @Test fun `empty magazine returns failure without spending energy or playing effects`() {
        val f = fixture(BlowpipeCharges.variants.first().first, 0)
        assertFalse(complete(f.action))
        assertEquals(listOf("stopCombat"), mockingDetails(f.manager).invocations.map { it.method.name })
        assertEquals(0, f.queues.size)
        assertTrue(mockingDetails(f.access).invocations.none { it.method.name == "anim" || it.method.name == "spotanim" })
    }

    @Test fun `last shot still succeeds and delayed healing cannot affect a replacement login`() {
        val f = fixture(BlowpipeCharges.variants.first().first, 1)
        assertTrue(complete(f.action))
        assertFalse(BlowpipeCharges.read(f.player.worn[Wearpos.RightHand.slot]).ready)
        verify(f.manager).stopCombat(f.access)
        f.player.uuid = 2
        f.player.assignUid()
        f.queues.iterator().next().action()
        assertEquals(50.toByte(), f.player.statMap.getCurrentLevel("stat.hitpoints"))
    }
    private data class Fixture(val player: Player, val access: ProtectedAccess, val manager: SpecialAttackManager, val target: Npc, val attack: CombatAttack.Ranged, val queues: WorldQueueList, val special: SpecialAttack.Ranged, val action: suspend () -> Boolean)
    private fun fixture(symbol: String, count: Int): Fixture {
        val player = Player().apply {
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
            slotId = 1; uuid = 1; assignUid()
            statMap.setBaseLevel("stat.hitpoints", 99.toByte())
            statMap.setCurrentLevel("stat.hitpoints", 50.toByte())
        }
        val item = BlowpipeCharges.write(InvObj(symbol), BlowpipeCharges.Contents(if (count > 0) 8 else 0, count, 10))
        player.worn[Wearpos.RightHand.slot] = item
        val access = mock(ProtectedAccess::class.java)
        val random = mock(GameRandom::class.java)
        `when`(access.player).thenReturn(player)
        `when`(access.random).thenReturn(random)
        `when`(random.of(3)).thenReturn(1)
        val manager = mock(SpecialAttackManager::class.java)
        val queues = WorldQueueList()
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }.also { it.isAccessible = true }.invoke(weapons)
        val registry = SpecialAttackRegistry(weapons)
        with(BlowpipeSpecialAttacks(queues)) { SpecialAttackRepository(registry).register(manager) }
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
        val attack = CombatAttack.Ranged(item, RangedAttackType.Light, RangedAttackStyle.Rapid)
        val dart = getInvObj(InvObj("obj.dragon_dart"))
        val travel = RSCM.getReverseMapping(RSCMType.SPOTANIM, dart.param(params.proj_travel).id)
        val proj = mock(ProjAnim::class.java)
        `when`(proj.clientCycles).thenReturn(45)
        `when`(proj.serverCycles).thenReturn(2)
        `when`(manager.spawnProjectile(access, target, travel, "projanim.thrown")).thenReturn(proj)
        `when`(manager.rollRangedDamage(access, target, attack, 2.0, 1.5)).thenReturn(21)
        val hit = mock(Hit::class.java)
        `when`(hit.damage).thenReturn(21)
        `when`(manager.queueRangedHit(access, target, dart, 21, 45, 2)).thenReturn(hit)
        val special = registry[item] as SpecialAttack.Ranged
        return Fixture(player, access, manager, target, attack, queues, special) { special.attack(access, target, attack) }
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
        @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() }
    }
}
