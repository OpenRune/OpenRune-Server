package org.rsmod.content.other.special.attacks

import dev.openrune.ServerCacheManager
import dev.openrune.definition.codec.SpotAnimCodec
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
import org.rsmod.api.combat.commons.*
import org.rsmod.api.combat.commons.styles.*
import org.rsmod.api.combat.commons.types.*
import org.rsmod.api.combat.manager.*
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.content.quest.manager.*
import org.rsmod.game.damage.DamageContributions
import org.rsmod.game.entity.*
import org.rsmod.game.hit.*
import org.rsmod.game.inv.*
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.getInvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class DorgeshuunSpecialAttacksTest {
    private lateinit var previousPolicy: QuestRequirementPolicy

    @BeforeEach fun policy() {
        previousPolicy = QuestRequirements.activePolicy()
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.AssumeCompleted))
    }

    @AfterEach fun restorePolicy() { QuestRequirements.install(previousPolicy) }

    @Test fun `last positive damager is independent of totals and resets with encounter contributions`() {
        val contributions = DamageContributions()
        val first = player(1)
        val second = player(2)
        assertFalse(contributions.wasLastDamagedBy(first))
        contributions.record(first, 100)
        contributions.record(second, 1)
        assertTrue(contributions.wasLastDamagedBy(second))
        contributions.record(first, 0)
        assertTrue(contributions.wasLastDamagedBy(second))
        contributions.record(first, 1)
        assertTrue(contributions.wasLastDamagedBy(first))
        assertEquals(101, contributions.damageBy(first))
        val imported = DamageContributions().apply { record(second, 30) }
        contributions.absorbFrom(imported)
        assertTrue(contributions.wasLastDamagedBy(first), "importing totals is not a new hit")
        contributions.record(npc(), 1)
        assertFalse(contributions.wasLastDamagedBy(first))
        contributions.clear()
        assertFalse(contributions.wasLastDamagedBy(second))
        assertTrue(contributions.isEmpty)
    }

    @Test fun `all bone daggers guarantee only the first or another attackers followup hit`() {
        for (weapon in DorgeshuunSpecialAttacks.DAGGERS) for (last in listOf(0, 1, 2)) {
            val f = fixture()
            val item = InvObj(weapon)
            val attack = CombatAttack.Melee(item, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)
            if (last != 0) f.target.recordDamage(if (last == 1) f.source else player(2), 1)
            `when`(f.manager.rollMeleeMaxHit(f.access, f.target, attack.type, attack.style, 1.0)).thenReturn(10)
            val damage = if (last == 1) 0 else 10
            val hit = hit(damage)
            `when`(f.manager.queueMeleeHit(f.access, f.target, damage, 1)).thenReturn(hit)
            val special = f.registry[item] as SpecialAttack.Melee
            assertEquals(750, special.energyInHundreds)
            assertTrue(run { special.attack(f.access, f.target, attack) })
            assertEquals(if (last == 1) 1 else 0, mockingDetails(f.manager).invocations.count { it.method.name == "rollMeleeAccuracy" })
            verify(f.access).anim("seq.dttd_player_stab_bone_dagger", 0)
            verify(f.access).spotanim("spotanim.dttd_dagger_sp_attack_spotanim", 0, 0, constants.spotanim_slot_combat)
            assertEquals(100, f.target.defenceLvl)
            hit.impactEffects.complete(damage)
            assertEquals(100 - damage, f.target.defenceLvl)
        }
    }

    @Test fun `quest rejection precedes animations hits and ammunition usage`() {
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.RespectProgress))
        val f = fixture()
        for (weapon in DorgeshuunSpecialAttacks.DAGGERS) {
            val item = InvObj(weapon)
            val special = f.registry[item] as SpecialAttack.Melee
            assertFalse(run { special.attack(f.access, f.target, CombatAttack.Melee(item, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)) })
        }
        val crossbow = InvObj("obj.dttd_bone_crossbow")
        val special = f.registry[crossbow] as SpecialAttack.Ranged
        assertFalse(run { special.attack(f.access, f.target, CombatAttack.Ranged(crossbow, RangedAttackType.Standard, RangedAttackStyle.Accurate)) })
        verifyNoInteractions(f.effects, f.ammo)
        assertFalse(mockingDetails(f.access).invocations.any { it.method.name in listOf("anim", "spotanim") })
        assertFalse(mockingDetails(f.manager).invocations.any { it.method.name.startsWith("queue") })
        QuestRequirements.install(QuestRequirementPolicy(QuestRequirementMode.VirtualCompletions, setOf(DorgeshuunSpecialAttacks.QUEST)))
        assertTrue(QuestRequirements.hasCompleted(f.source, DorgeshuunSpecialAttacks.QUEST))
    }

    @Test fun `Snipe uses bone special projectile consumes one bolt and drains only at impact`() {
        val f = fixture()
        val crossbow = InvObj("obj.dttd_bone_crossbow")
        val bolts = InvObj("obj.dttd_bone_crossbow_bolt", 2)
        f.source.worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        f.source.worn[Wearpos.Quiver.slot] = bolts
        val ammo = getInvObj(bolts)
        val weapon = getInvObj(crossbow)
        `when`(f.ammo.attemptAmmoUsage(f.source, weapon, ammo)).thenReturn(true)
        val attack = CombatAttack.Ranged(crossbow, RangedAttackType.Standard, RangedAttackStyle.Accurate)
        val trajectory = RSCM.getReverseMapping(RSCMType.PROJANIM, weapon.param(params.proj_type).id)
        val projectile = mock(ProjAnim::class.java)
        `when`(projectile.clientCycles).thenReturn(60)
        `when`(projectile.serverCycles).thenReturn(3)
        `when`(f.manager.spawnProjectile(f.access, f.target, "spotanim.dttd_bone_crossbowbolt_travel_sp_attack", trajectory)).thenReturn(projectile)
        `when`(f.manager.rollRangedMaxHit(f.access, f.target, attack.type, attack.style, 1.0, 0)).thenReturn(20)
        val hit = hit(20)
        `when`(f.manager.queueRangedHit(f.access, f.target, ammo, 20, 60, 3)).thenReturn(hit)
        val special = f.registry[crossbow] as SpecialAttack.Ranged
        assertEquals(750, special.energyInHundreds)
        assertTrue(run { special.attack(f.access, f.target, attack) })
        verify(f.effects).playWeaponFx(f.source, attack)
        assertFalse(mockingDetails(f.manager).invocations.any { it.method.name == "rollRangedAccuracy" })
        assertEquals(1, mockingDetails(f.ammo).invocations.count { it.method.name.startsWith("useQuiverAmmo") })
        assertEquals(100, f.target.defenceLvl)
        hit.impactEffects.complete(12)
        assertEquals(88, f.target.defenceLvl)
    }

    @Test fun `drain does not stack ignores cancelled hits and rejects stale source logins`() {
        val source = player(1)
        val target = npc()
        val blocked = hit(20)
        DorgeshuunSpecialAttacks.attachDefenceDrain(blocked, source, target)
        blocked.impactEffects.complete(0)
        assertEquals(100, target.defenceLvl)
        val first = hit(20)
        DorgeshuunSpecialAttacks.attachDefenceDrain(first, source, target)
        first.impactEffects.complete(12)
        first.impactEffects.complete(12)
        val second = hit(30)
        DorgeshuunSpecialAttacks.attachDefenceDrain(second, source, target)
        second.impactEffects.complete(30)
        assertEquals(88, target.defenceLvl)
        target.defenceLvl = 100
        val stale = hit(20)
        DorgeshuunSpecialAttacks.attachDefenceDrain(stale, source, target)
        source.uuid = 9; source.assignUid()
        stale.impactEffects.complete(20)
        assertEquals(100, target.defenceLvl)
    }

    @Test fun `PvP drain uses applied damage and leaves already lowered defence unchanged`() {
        val source = player(1)
        val target = player(2)
        target.statMap.setBaseLevel("stat.defence", 99.toByte())
        target.statMap.setCurrentLevel("stat.defence", 99.toByte())
        val first = hit(30)
        DorgeshuunSpecialAttacks.attachDefenceDrain(first, source, target)
        first.impactEffects.complete(7)
        assertEquals(92, target.statMap.getCurrentLevel("stat.defence").toInt())
        val next = hit(30)
        DorgeshuunSpecialAttacks.attachDefenceDrain(next, source, target)
        next.impactEffects.complete(30)
        assertEquals(92, target.statMap.getCurrentLevel("stat.defence").toInt())
    }

    @Test fun `Snipe with no ammunition stops before any visual or hit`() {
        val f = fixture()
        f.source.worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        val item = InvObj("obj.dttd_bone_crossbow")
        val special = f.registry[item] as SpecialAttack.Ranged
        assertFalse(run { special.attack(f.access, f.target, CombatAttack.Ranged(item, RangedAttackType.Standard, RangedAttackStyle.Accurate)) })
        verifyNoInteractions(f.effects)
        assertFalse(mockingDetails(f.manager).invocations.any { it.method.name.startsWith("queue") || it.method.name == "spawnProjectile" })
        assertFalse(mockingDetails(f.ammo).invocations.any { it.method.name.startsWith("useQuiverAmmo") })
    }

    @Test fun `bone dagger graphic sequence is not reused as the player animation`() {
        val cache = ServerCacheManager.init(240)
        try {
            val id = "spotanim.dttd_dagger_sp_attack_spotanim".asRSCM()
            val graphic = SpotAnimCodec(240).loadData(id, cache.data(2, 13, id))
            assertNotEquals(graphic.animationId, "seq.dttd_player_stab_bone_dagger".asRSCM())
        } finally { cache.close() }
    }

    private fun fixture(): Fixture {
        val source = player(1)
        val target = npc()
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(source)
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }.also { it.isAccessible = true }.invoke(weapons)
        val registry = SpecialAttackRegistry(weapons)
        val manager = mock(SpecialAttackManager::class.java)
        val effects = mock(PlayerAttackManager::class.java)
        val ammo = mock(RangedAmmoManager::class.java)
        with(DorgeshuunSpecialAttacks(effects, ammo)) { SpecialAttackRepository(registry).register(manager) }
        return Fixture(source, target, access, manager, effects, ammo, registry)
    }
    private data class Fixture(val source: Player, val target: Npc, val access: ProtectedAccess,
        val manager: SpecialAttackManager, val effects: PlayerAttackManager, val ammo: RangedAmmoManager,
        val registry: SpecialAttackRegistry)
    private fun player(id: Long) = Player().apply { slotId = id.toInt(); uuid = id; assignUid() }
    private fun npc() = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }).apply {
        slotId = 3; assignUid(); defenceLvl = 100; baseDefenceLvl = 100
    }
    private fun hit(damage: Int) = Hit(HitType.Melee, Hitmark(0).copy(damage = damage), null, null, null)
    private fun run(block: suspend () -> Boolean): Boolean {
        var completed: Result<Boolean>? = null
        block.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { completed = result }
        })
        return checkNotNull(completed).getOrThrow()
    }
    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
