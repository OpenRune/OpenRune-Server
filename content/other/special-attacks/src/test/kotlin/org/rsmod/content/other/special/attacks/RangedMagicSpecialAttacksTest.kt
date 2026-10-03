package org.rsmod.content.other.special.attacks

import dev.openrune.ServerCacheManager
import dev.openrune.filesystem.Cache
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
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.content.other.special.attacks.magic.*
import org.rsmod.content.other.special.attacks.ranged.*
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.queue.WorldQueueList
import org.rsmod.game.type.getInvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(InternalApi::class)
class RangedMagicSpecialAttacksTest {
    private lateinit var cache: Cache
    private lateinit var weapons: SpecialAttackWeapons

    @BeforeAll fun loadCache() {
        cache = ServerCacheManager.init(240)
        weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }
            .also { it.isAccessible = true }.invoke(weapons)
    }

    @AfterAll fun closeCache() { if (::cache.isInitialized) cache.close() }

    @Test fun `ranged aliases register current cache energy and valid animations`() {
        val registry = register(RangedWeaponSpecialAttacks(mock(RangedAmmoManager::class.java), WorldQueueList()), mock(SpecialAttackManager::class.java))
        for (spec in RangedWeaponSpec.entries) {
            assertTrue(spec.animation.asRSCM(RSCMType.SEQ) >= 0)
            spec.launch?.let { assertTrue(it.asRSCM(RSCMType.SPOTANIM) >= 0) }
            spec.travel?.let { assertTrue(it.asRSCM(RSCMType.SPOTANIM) >= 0) }
            for (symbol in spec.weapons) {
                val item = InvObj(symbol)
                val special = registry[item] as SpecialAttack.Ranged
                assertEquals(weapons.getSpecialEnergy(item.id), special.energyInHundreds, symbol)
                Assertions.assertNotNull(getInvObj(item).paramOrNull(params.proj_type), symbol)
            }
        }
    }

    @Test fun `nightmare aliases register magic handlers with cache costs and FX`() {
        val registry = register(NightmareStaffSpecialAttacks(WorldQueueList()), mock(SpecialAttackManager::class.java))
        for (symbol in NightmareStaffSpecialAttacks.VOLATILE + "obj.nightmare_staff_eldritch") {
            val item = InvObj(symbol)
            assertEquals(550, (registry[item] as SpecialAttack.Magic).energyInHundreds)
        }
        for (symbol in listOf("seq.nightmare_staff_eldritch_player_cast", "seq.nightmare_staff_volatile_cast")) assertTrue(symbol.asRSCM(RSCMType.SEQ) >= 0)
        for (name in listOf("eldritch", "volatile")) for (part in listOf("cast", "hit")) assertTrue("spotanim.nightmare_staff_${name}_${part}_spotanim".asRSCM(RSCMType.SPOTANIM) >= 0)
        assertTrue("spotanim.failedspell_impact".asRSCM(RSCMType.SPOTANIM) >= 0)
    }

    @Test fun `snapshot rejects one arrow before animation damage or consumption`() {
        val fixture = fixture("obj.magic_shortbow", "obj.rune_arrow", 1)
        assertFalse(run(fixture.action))
        verify(fixture.manager).stopCombat(fixture.access)
        verify(fixture.manager, never()).continueCombat(fixture.access, fixture.target)
        assertEquals(0, mockingDetails(fixture.manager).invocations.count { it.method.name == "queueRangedHit" })
        assertEquals(0, mockingDetails(fixture.ammo).invocations.count { it.method.name.startsWith("useQuiverAmmo") })
    }

    @Test fun `snapshot queues two independently rolled hits and consumes two arrows`() {
        val fixture = fixture("obj.magic_shortbow", "obj.rune_arrow", 2)
        `when`(fixture.manager.rollRangedAccuracy(fixture.access, fixture.target, RangedAttackType.Standard, RangedAttackStyle.Accurate, RangedAttackType.Standard, 10.0 / 7.0)).thenReturn(false, true)
        assertTrue(run(fixture.action))
        val hits = mockingDetails(fixture.manager).invocations.filter { it.method.name == "queueRangedHit" }
        assertEquals(listOf(0, 12), hits.map { it.arguments[3] })
        assertEquals(2, mockingDetails(fixture.ammo).invocations.count { it.method.name.startsWith("useQuiverAmmo") })
    }

    @Test fun `powershot never performs an accuracy check and uses ammo only formula`() {
        val fixture = fixture("obj.magic_longbow", "obj.rune_arrow", 1)
        assertTrue(run(fixture.action))
        assertEquals(0, mockingDetails(fixture.manager).invocations.count { it.method.name == "rollRangedAccuracy" })
        assertEquals(0, mockingDetails(fixture.manager).invocations.count { it.method.name == "rollRangedMaxHit" })
        assertEquals(19, RangedSpecialRules.ammoOnlyMaxHit(99, 49))
        assertEquals(1, mockingDetails(fixture.ammo).invocations.count { it.method.name.startsWith("useQuiverAmmo") })
    }

    @Test fun `dragon knife variants use the duality animation and correct poisoned projectiles`() {
        for (spec in listOf(RangedWeaponSpec.DragonKnife, RangedWeaponSpec.PoisonedDragonKnife)) {
            for (weapon in spec.weapons) {
                val fixture = fixture(weapon, weapon, 2, thrown = true)
                assertTrue(run(fixture.action))
                verify(fixture.access).anim(spec.animation, 0)
                assertNotEquals("seq.human_dragon_knife".asRSCM(), spec.animation.asRSCM())
                val projectiles = mockingDetails(fixture.manager).invocations.filter { it.method.name == "spawnProjectile" }
                assertEquals(2, projectiles.size)
                assertTrue(projectiles.all { it.arguments[2] == spec.travel })
                assertEquals(2, mockingDetails(fixture.manager).invocations.count { it.method.name == "queueRangedHit" })
                assertEquals(2, mockingDetails(fixture.ammo).invocations.count { it.method.name.startsWith("useThrownWeapon") })
                assertEquals(0, mockingDetails(fixture.ammo).invocations.count { it.method.name.startsWith("useQuiverAmmo") })
            }
        }
        assertNotEquals(RangedWeaponSpec.DragonKnife.animation, RangedWeaponSpec.PoisonedDragonKnife.animation)
        assertNotEquals(RangedWeaponSpec.DragonKnife.travel, RangedWeaponSpec.PoisonedDragonKnife.travel)
    }

    @Test fun `duality rejects a single knife without throwing or animating`() {
        val fixture = fixture("obj.dragon_knife", "obj.dragon_knife", 1, thrown = true)
        assertFalse(run(fixture.action))
        assertFalse(mockingDetails(fixture.access).invocations.any { it.method.name == "anim" })
        assertFalse(mockingDetails(fixture.ammo).invocations.any { it.method.name.startsWith("useThrownWeapon") })
    }

    @Test fun `accurate powershot zero roll becomes one while a snapshot miss remains zero`() {
        val fixture = fixture("obj.magic_longbow", "obj.rune_arrow", 1)
        `when`(fixture.access.random.of(0..19)).thenReturn(0)
        assertTrue(run(fixture.action))
        val hits = mockingDetails(fixture.manager).invocations.filter { it.method.name == "queueRangedHit" }
        assertEquals(listOf(1), hits.map { it.arguments[3] })
    }

    @Test fun `nightmare scaling caps and prayer restoration never reduces boosted prayer`() {
        assertEquals(58, NightmareStaffRules.baseMaxHit(99, false))
        assertEquals(58, NightmareStaffRules.baseMaxHit(120, false))
        assertEquals(44, NightmareStaffRules.baseMaxHit(99, true))
        assertEquals(10, NightmareStaffRules.prayerRestoration(110, 22))
        assertEquals(0, NightmareStaffRules.prayerRestoration(125, 22))
        assertEquals(22, NightmareStaffRules.prayerRestoration(50, 22))
    }

    @Test fun `volatile miss queues zero damage with no maximum hit roll`() {
        val fixture = nightmareFixture(false)
        assertTrue(run(fixture.action))
        verify(fixture.manager).queueMagicHit(fixture.access, fixture.target, 0, 60, 2, null)
        assertEquals(0, mockingDetails(fixture.manager).invocations.count { it.method.name == "rollStaffMaxHit" })
        verify(fixture.manager).setNextAttackDelay(fixture.access, 5)
        verify(fixture.manager).continueCombat(fixture.access, fixture.target as PathingEntity)
    }

    @Test fun `volatile accurate special uses scaled magic maximum and doubled tick hit delay`() {
        val fixture = nightmareFixture(true)
        assertTrue(run(fixture.action))
        verify(fixture.manager).rollStaffMaxHit(fixture.access, fixture.target, 58, 1.0)
        verify(fixture.manager).queueMagicHit(fixture.access, fixture.target, 40, 60, 2, null)
    }

    private fun register(map: SpecialAttackMap, manager: SpecialAttackManager): SpecialAttackRegistry {
        val registry = SpecialAttackRegistry(weapons)
        with(map) { SpecialAttackRepository(registry).register(manager) }
        return registry
    }

    private fun fixture(weapon: String, ammunition: String, count: Int, thrown: Boolean = false): Fixture {
        val manager = mock(SpecialAttackManager::class.java)
        val ammoManager = mock(RangedAmmoManager::class.java)
        val access = mock(ProtectedAccess::class.java)
        val random = mock(GameRandom::class.java)
        val player = Player().apply {
            statMap.setCurrentLevel("stat.ranged", 99.toByte())
            worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14))
        }
        val item = InvObj(weapon, if (thrown) count else 1)
        val ammo = InvObj(ammunition, count)
        player.worn[Wearpos.RightHand.slot] = item
        if (!thrown) player.worn[Wearpos.Quiver.slot] = ammo
        `when`(access.player).thenReturn(player)
        `when`(access.random).thenReturn(random)
        `when`(random.of(0..19)).thenReturn(12)
        `when`(ammoManager.attemptAmmoUsage(player, getInvObj(item), getInvObj(ammo))).thenReturn(true)
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
        val travel = if (thrown) RangedWeaponSpec.entries.single { weapon in it.weapons }.travel!! else RSCM.getReverseMapping(RSCMType.SPOTANIM, getInvObj(ammo).param(params.proj_travel).id)
        val proj = mock(ProjAnim::class.java)
        `when`(proj.clientCycles).thenReturn(30)
        `when`(proj.serverCycles).thenReturn(2)
        val normalTrajectory = RSCM.getReverseMapping(RSCMType.PROJANIM, getInvObj(item).param(params.proj_type).id)
        for (path in listOf("projanim.doublearrow_one", "projanim.doublearrow_two", normalTrajectory)) `when`(manager.spawnProjectile(access, target, travel, path)).thenReturn(proj)
        for (damage in listOf(0, 1, 12)) {
            val hit = mock(Hit::class.java)
            `when`(hit.damage).thenReturn(damage)
            `when`(manager.queueRangedHit(access, target, if (thrown) null else getInvObj(ammo), damage, 30, 2)).thenReturn(hit)
        }
        val special = register(RangedWeaponSpecialAttacks(ammoManager, WorldQueueList()), manager)[item] as SpecialAttack.Ranged
        val attack = CombatAttack.Ranged(item, RangedAttackType.Standard, RangedAttackStyle.Accurate)
        return Fixture(manager, ammoManager, access, target) { special.attack(access, target, attack) }
    }

    private class Fixture(val manager: SpecialAttackManager, val ammo: RangedAmmoManager, val access: ProtectedAccess, val target: Npc, val action: suspend () -> Boolean)

    private fun nightmareFixture(accurate: Boolean): Fixture {
        val manager = mock(SpecialAttackManager::class.java)
        val access = mock(ProtectedAccess::class.java)
        val player = Player()
        player.statMap.setCurrentLevel("stat.magic", 99.toByte())
        `when`(access.player).thenReturn(player)
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
        `when`(manager.rollStaffAccuracy(access, target, null, 1.5)).thenReturn(accurate)
        `when`(manager.rollStaffMaxHit(access, target, 58, 1.0)).thenReturn(40)
        val hit = mock(Hit::class.java)
        val damage = if (accurate) 40 else 0
        `when`(hit.damage).thenReturn(damage)
        `when`(manager.queueMagicHit(access, target, damage, 60, 2, null)).thenReturn(hit)
        val item = InvObj("obj.nightmare_staff_volatile")
        val special = register(NightmareStaffSpecialAttacks(WorldQueueList()), manager)[item] as SpecialAttack.Magic
        return Fixture(manager, mock(RangedAmmoManager::class.java), access, target) { special.attack(access, target, CombatAttack.Staff(item, null)) }
    }
    private fun run(action: suspend () -> Boolean): Boolean {
        var result: Result<Boolean>? = null
        action.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<Boolean>) { result = value }
        })
        return checkNotNull(result).getOrThrow()
    }
}
