package org.rsmod.content.other.special.attacks.melee

import com.google.inject.spi.Elements
import com.google.inject.spi.LinkedKeyBinding
import dev.openrune.ServerCacheManager
import dev.openrune.filesystem.Cache
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.enums.SaEnums
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.worn.DragonfireShields
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttack
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRegistry
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.content.other.special.attacks.SpecialAttackModule
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit
import org.rsmod.game.inv.InvObj
import org.rsmod.annotations.InternalApi
import org.rsmod.map.CoordGrid

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@OptIn(InternalApi::class)
class MeleeSpecialAttacksTest {
    private lateinit var cache: Cache
    private lateinit var weapons: SpecialAttackWeapons

    @BeforeAll fun loadCache() {
        cache = ServerCacheManager.init(240)
        weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }
            .also { it.isAccessible = true }.invoke(weapons)
    }

    @AfterAll fun closeCache() { if (::cache.isInitialized) cache.close() }

    @Test fun `all explicit melee mappings and animation symbols exist in current cache`() {
        val manager = mock(SpecialAttackManager::class.java)
        val registry = register(MeleeWeaponSpecialAttacks(mock(org.rsmod.api.random.GameRandom::class.java), mock(MeleeSpecialDamage::class.java)), manager)
        val all = MeleeWeaponSpec.entries.flatMap { it.weapons }
        assertEquals(all.size, all.toSet().size)
        for (spec in MeleeWeaponSpec.entries) {
            assertTrue(spec.animation.asRSCM(RSCMType.SEQ) >= 0)
            assertTrue(spec.spot.asRSCM(RSCMType.SPOTANIM) >= 0)
            for (weapon in spec.weapons) {
                val id = weapon.asRSCM(RSCMType.OBJ)
                val mapped = registry[InvObj(weapon)] as SpecialAttack.Melee
                assertEquals(weapons.getSpecialEnergy(id), mapped.energyInHundreds, weapon)
            }
        }
    }

    @Test fun `elder maul and ornament now resolve to actual attacks`() {
        val manager = mock(SpecialAttackManager::class.java)
        val registry = register(MeleeWeaponSpecialAttacks(mock(org.rsmod.api.random.GameRandom::class.java), mock(MeleeSpecialDamage::class.java)), manager)
        for (weapon in listOf("obj.elder_maul", "obj.elder_maul_ornament", "obj.br_elder_maul")) {
            assertInstanceOf(SpecialAttack.Melee::class.java, registry[InvObj(weapon)])
        }
    }

    @Test fun `elder defence reduction stacks from current level and rounds down`() {
        val target = npc().apply { defenceLvl = 75 }
        val source = Player()
        MeleeWeaponSpecialAttacks.applyEffect(source, target, 10, MeleeEffect.ElderMaul)
        assertEquals(49, target.defenceLvl)
        MeleeWeaponSpecialAttacks.applyEffect(source, target, 10, MeleeEffect.ElderMaul)
        assertEquals(32, target.defenceLvl)
        target.defenceLvl = 1
        MeleeWeaponSpecialAttacks.applyEffect(source, target, 10, MeleeEffect.ElderMaul)
        assertEquals(1, target.defenceLvl)
    }

    @Test fun `bandos drain exhausts defence then other combat stats without negative levels`() {
        val target = npc().apply { defenceLvl = 10; strengthLvl = 8; attackLvl = 20; magicLvl = 40; rangedLvl = 30 }
        MeleeWeaponSpecialAttacks.applyEffect(Player(), target, 25, MeleeEffect.Bandos)
        assertEquals(listOf(0, 0, 13, 40, 30), listOf(target.defenceLvl, target.strengthLvl, target.attackLvl, target.magicLvl, target.rangedLvl))
        MeleeWeaponSpecialAttacks.applyEffect(Player(), target, 999, MeleeEffect.Bandos)
        assertEquals(listOf(0, 0, 0, 0, 0), listOf(target.defenceLvl, target.strengthLvl, target.attackLvl, target.magicLvl, target.rangedLvl))
    }

    @Test fun `whip transfers current run energy and caps the attackers energy`() {
        val source = Player().apply { runEnergy = 9_950 }
        val target = Player().apply { runEnergy = 2_000 }
        MeleeWeaponSpecialAttacks.applyEffect(source, target, 8, MeleeEffect.Whip)
        assertEquals(1_800, target.runEnergy)
        assertEquals(10_000, source.runEnergy)
    }

    @Test fun `dragon dagger independently rolls second hit after missing first`() {
        val fixture = attackFixture("obj.dragon_dagger")
        `when`(fixture.manager.rollMeleeAccuracy(fixture.access, fixture.target, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, MeleeAttackType.Slash, 1.15))
            .thenReturn(false, true)
        `when`(fixture.manager.rollMeleeMaxHit(fixture.access, fixture.target, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, 1.15)).thenReturn(10)
        fixture.run()
        verify(fixture.manager).queueMeleeHit(fixture.access, fixture.target, 0, 1)
        verify(fixture.manager).queueMeleeHit(fixture.access, fixture.target, 10, 1)
    }

    @Test fun `abyssal dagger shares one accuracy roll for both hits`() {
        val fixture = attackFixture("obj.abyssal_dagger")
        `when`(fixture.manager.rollMeleeAccuracy(fixture.access, fixture.target, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, MeleeAttackType.Stab, 1.25)).thenReturn(true)
        `when`(fixture.manager.rollMeleeMaxHit(fixture.access, fixture.target, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, 0.85)).thenReturn(10)
        fixture.run()
        verify(fixture.manager, times(1)).rollMeleeAccuracy(fixture.access, fixture.target, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, MeleeAttackType.Stab, 1.25)
        verify(fixture.manager, times(2)).queueMeleeHit(fixture.access, fixture.target, 10, 1)
    }

    @Test fun `halberd second hit is limited to larger targets and sweep stays in three tiles`() {
        assertEquals(1, MeleeSpecialRules.halberdHits(1))
        assertEquals(2, MeleeSpecialRules.halberdHits(5))
        val source = CoordGrid(3200, 3200, 0)
        val north = CoordGrid(3200, 3202, 0)
        assertTrue(MeleeSpecialRules.inHalberdSweep(source, north, CoordGrid(3199, 3202, 0)))
        assertTrue(MeleeSpecialRules.inHalberdSweep(source, north, CoordGrid(3201, 3202, 0)))
        assertFalse(MeleeSpecialRules.inHalberdSweep(source, north, CoordGrid(3202, 3202, 0)))
        assertFalse(MeleeSpecialRules.inHalberdSweep(source, north, CoordGrid(3200, 3201, 0)))
        assertFalse(MeleeSpecialRules.inHalberdSweep(source, north, CoordGrid(3200, 3202, 1)))
    }

    @Test fun `all four halberd variants resolve to the same sweep implementation with cache costs`() {
        val manager = mock(SpecialAttackManager::class.java)
        val registry = register(HalberdSpecialAttacks(mock(MeleeAreaTargets::class.java)), manager)
        for (weapon in HalberdSpecialAttacks.WEAPONS) {
            val mapped = registry[InvObj(weapon)] as SpecialAttack.Melee
            assertEquals(300, mapped.energyInHundreds)
        }
    }

    @Test fun `registered crystal halberd actually queues both large target hits with separate accuracy`() {
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.size > 1 })
        val manager = mock(SpecialAttackManager::class.java)
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(Player())
        `when`(access.coords).thenReturn(CoordGrid(3200, 3200, 0))
        val item = InvObj("obj.crystal_halberd")
        val attack = CombatAttack.Melee(item, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)
        for ((accuracy, delay) in listOf(1.0 to 1, 0.75 to 2)) {
            `when`(manager.rollMeleeDamage(access, target, attack, accuracy, 1.1,
                attack.type, attack.style, MeleeAttackType.Slash)).thenReturn(10)
            val hit = mock(Hit::class.java)
            `when`(hit.damage).thenReturn(10)
            `when`(manager.queueMeleeHit(access, target, 10, delay)).thenReturn(hit)
        }
        val special = register(HalberdSpecialAttacks(mock(MeleeAreaTargets::class.java)), manager)[item] as SpecialAttack.Melee
        Fixture(manager, access, target) { special.attack(access, target, attack) }.run()
        verify(manager).queueMeleeHit(access, target, 10, 1)
        verify(manager).queueMeleeHit(access, target, 10, 2)
        verify(manager).continueCombat(access, target as org.rsmod.game.entity.PathingEntity)
    }

    @Test fun `elder and four godswords execute registered handlers instead of an empty mapping`() {
        for (spec in listOf(MeleeWeaponSpec.ElderMaul, MeleeWeaponSpec.ArmadylGodsword,
            MeleeWeaponSpec.BandosGodsword, MeleeWeaponSpec.SaradominGodsword, MeleeWeaponSpec.ZamorakGodsword)) {
            val fixture = attackFixture(spec.weapons.first())
            `when`(fixture.manager.rollMeleeAccuracy(fixture.access, fixture.target, MeleeAttackType.Stab,
                MeleeAttackStyle.Accurate, spec.blockType, spec.accuracyMultiplier)).thenReturn(true)
            `when`(fixture.manager.rollMeleeMaxHit(fixture.access, fixture.target, MeleeAttackType.Stab,
                MeleeAttackStyle.Accurate, spec.damageMultiplier)).thenReturn(10)
            fixture.run()
            verify(fixture.manager).queueMeleeHit(fixture.access, fixture.target, 10,
                if (spec.effect == MeleeEffect.ElderMaul) 2 else 1)
        }
        assertEquals(58, MeleeSpecialDamage.scaledMaximum(43, 110, 125),
            "Godswords floor their 10 percent bonus before the second multiplier")
    }

    @Test fun `granite hammer still queues five damage on an accuracy miss`() {
        val fixture = attackFixture("obj.granite_hammer")
        fixture.run()
        verify(fixture.manager).queueMeleeHit(fixture.access, fixture.target, 5, 1)
    }

    @Test fun `anchor drains only first eligible stat and never spills or lowers below one`() {
        val target = npc().apply { defenceLvl = 2; attackLvl = 20; rangedLvl = 30; magicLvl = 40 }
        MeleeWeaponSpecialAttacks.applyEffect(Player(), target, 100, MeleeEffect.Anchor)
        assertEquals(1, target.defenceLvl)
        assertEquals(20, target.attackLvl)
        MeleeWeaponSpecialAttacks.applyEffect(Player(), target, 50, MeleeEffect.Anchor)
        assertEquals(15, target.attackLvl)
    }

    @Test fun `registered voidwaker queues magic damage from melee maximum without accuracy roll`() {
        val manager = mock(SpecialAttackManager::class.java)
        val access = mock(ProtectedAccess::class.java)
        val player = Player()
        val target = npc()
        `when`(access.player).thenReturn(player)
        val item = InvObj("obj.voidwaker")
        val attack = CombatAttack.Melee(item, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)
        val damage = mock(MeleeSpecialDamage::class.java)
        val random = mock(GameRandom::class.java)
        `when`(damage.maximum(player, target, attack, 100, 100, true)).thenReturn(40)
        `when`(random.of(20, 60)).thenReturn(50)
        val hit = mock(Hit::class.java)
        `when`(hit.damage).thenReturn(50)
        `when`(manager.queueMagicHit(access, target, 50, 30, 1, null)).thenReturn(hit)
        val special = register(VoidwakerSpecialAttack(damage, random), manager)[item] as SpecialAttack.Melee
        Fixture(manager, access, target) { special.attack(access, target, attack) }.run()
        verify(manager).queueMagicHit(access, target, 50, 30, 1, null)
        verify(manager, never()).rollMeleeAccuracy(access, target, attack.type, attack.style, attack.type, 1.0)
    }

    @Test fun `actual module bindings register together and export every cache special item`() {
        val registry = SpecialAttackRegistry(weapons)
        val manager = mock(SpecialAttackManager::class.java)
        val implementations = Elements.getElements(SpecialAttackModule())
            .filterIsInstance<LinkedKeyBinding<*>>()
            .filter { it.key.typeLiteral.rawType == SpecialAttackMap::class.java }
            .map { it.linkedKey.typeLiteral.rawType }.toSet()
        assertTrue(implementations.size > 3, "Read the actual module bindings, including newly added maps")
        for (implementation in implementations) {
            val constructor = implementation.constructors.singleOrNull { it.isAnnotationPresent(jakarta.inject.Inject::class.java) }
                ?: implementation.getConstructor()
            val dependencies = constructor.parameterTypes.map { mock(it) }.toTypedArray()
            val map = constructor.newInstance(*dependencies) as SpecialAttackMap
            with(map) { SpecialAttackRepository(registry).register(manager) }
        }
        assertInstanceOf(SpecialAttack.Melee::class.java, registry[InvObj("obj.crystal_halberd")])
        assertInstanceOf(SpecialAttack.Melee::class.java, registry[InvObj("obj.elder_maul")])
        assertInstanceOf(SpecialAttack.Magic::class.java, registry[InvObj("obj.nightmare_staff_volatile")])
        val rows = mutableListOf("id\tname\tcategory\tenergy\tregistered_kind\thandler\tdescription")
        for ((item, energy) in SaEnums.sa_energy_requirements.filterValuesNotNull().sortedBy { it.key.id }) {
            val special = registry[InvObj(item)]
            val handler = when (special) {
                is SpecialAttack.Melee -> special.special.javaClass.name
                is SpecialAttack.Ranged -> special.special.javaClass.name
                is SpecialAttack.Magic -> special.special.javaClass.name
                is SpecialAttack.Instant -> special.special.javaClass.name
                is SpecialAttack.Shield -> special.special.javaClass.name
                null -> ""
            }
            val description = SaEnums.sa_descriptions.getOrNull(item).orEmpty().replace('\t', ' ').replace('\n', ' ')
            rows += "${item.id}\t${item.name}\t${item.weaponCategory}\t$energy\t${special?.javaClass?.simpleName ?: "missing"}\t$handler\t$description"
        }
        assertEquals(SaEnums.sa_energy_requirements.filterValuesNotNull().count(), rows.size - 1)
        val report = Path.of("content/other/special-attacks/build/reports/special-attack-coverage.tsv")
        Files.createDirectories(report.parent)
        Files.write(report, rows)
        // The energy enum only contains charged forms. Audit all six equipped operation forms too.
        val shieldRows = mutableListOf("id\tsymbol\tname\tcharged_form\tin_energy_enum\tregistered_kind\thandler")
        for (kind in DragonfireShields.Kind.entries) {
            for (symbol in listOf(kind.charged, kind.uncharged)) {
                val item = InvObj(symbol)
                val special = registry[item]
                assertInstanceOf(SpecialAttack.Shield::class.java, special, symbol)
                special as SpecialAttack.Shield
                val name = requireNotNull(ServerCacheManager.getItem(item.id)).name
                shieldRows += "${item.id}\t$symbol\t$name\t${symbol == kind.charged}\t${weapons.getSpecialEnergy(item.id) != null}\tShield\t${special.special.javaClass.name}"
            }
        }
        assertEquals(7, shieldRows.size)
        Files.write(report.resolveSibling("shield-special-coverage.tsv"), shieldRows)
    }

    private fun register(map: SpecialAttackMap, manager: SpecialAttackManager): SpecialAttackRegistry {
        val registry = SpecialAttackRegistry(weapons)
        with(map) { SpecialAttackRepository(registry).register(manager) }
        return registry
    }

    private fun npc(): Npc = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })

    @Test fun `registered warhammer drains only on impact and exactly once`() {
        val f = attackFixture("obj.dragon_warhammer")
        f.access.player.apply { slotId = 1; uuid = 1; assignUid() }
        f.target.apply { slotId = 2; assignUid(); defenceLvl = 100 }
        `when`(f.manager.rollMeleeAccuracy(f.access, f.target, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, MeleeAttackType.Crush, 1.0)).thenReturn(true)
        `when`(f.manager.rollMeleeMaxHit(f.access, f.target, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, 1.5)).thenReturn(10)
        f.run()
        assertEquals(100, f.target.defenceLvl)
        requireNotNull(f.queuedHit).impactEffects.complete(10)
        assertEquals(70, f.target.defenceLvl)
        f.queuedHit.impactEffects.complete(10)
        assertEquals(70, f.target.defenceLvl)
    }

    @Test fun `bandos drain uses applied damage and zero damage does not drain`() {
        val source = Player().apply { slotId = 1; uuid = 1; assignUid() }
        val target = npc().apply { slotId = 2; assignUid(); defenceLvl = 100 }
        val hit = Hit(org.rsmod.game.hit.HitType.Melee, org.rsmod.game.hit.Hitmark(0).copy(damage = 50), null, null, null)
        MeleeWeaponSpecialAttacks.attachEffect(hit, source, target, MeleeEffect.Bandos, mock(GameRandom::class.java))
        hit.impactEffects.complete(7)
        assertEquals(93, target.defenceLvl)
        val blocked = hit.copy(impactEffects = org.rsmod.game.hit.HitImpactEffects())
        MeleeWeaponSpecialAttacks.attachEffect(blocked, source, target, MeleeEffect.ElderMaul, mock(GameRandom::class.java))
        blocked.impactEffects.complete(0)
        assertEquals(93, target.defenceLvl)
    }

    @Test fun `healing waits for impact preserves pre-overkill basis and rejects replacement login`() {
        val source = Player().apply {
            slotId = 1; uuid = 1; assignUid()
            statMap.setBaseLevel("stat.hitpoints", 99); statMap.setCurrentLevel("stat.hitpoints", 20)
            statMap.setBaseLevel("stat.prayer", 99); statMap.setCurrentLevel("stat.prayer", 20)
        }
        val target = npc().apply { slotId = 2; assignUid() }
        fun pending(): Hit {
            val hit = Hit(org.rsmod.game.hit.HitType.Melee, org.rsmod.game.hit.Hitmark(0).copy(damage = 60), null, null, null)
            MeleeWeaponSpecialAttacks.attachEffect(hit, source, target, MeleeEffect.Saradomin, mock(GameRandom::class.java))
            return hit
        }
        val hit = pending()
        assertEquals(20.toByte(), source.statMap.getCurrentLevel("stat.hitpoints"))
        hit.impactEffects.complete(2)
        assertEquals(50.toByte(), source.statMap.getCurrentLevel("stat.hitpoints"))
        assertEquals(35.toByte(), source.statMap.getCurrentLevel("stat.prayer"))
        val oldLogin = pending()
        source.uuid = 2; source.assignUid()
        oldLogin.impactEffects.complete(60)
        assertEquals(50.toByte(), source.statMap.getCurrentLevel("stat.hitpoints"))
    }

    private fun attackFixture(weapon: String): Fixture {
        val manager = mock(SpecialAttackManager::class.java)
        val access = mock(ProtectedAccess::class.java)
        val player = Player()
        `when`(access.player).thenReturn(player)
        `when`(access.coords).thenReturn(CoordGrid(3200, 3200, 0))
        val target = npc()
        val zero = Hit(org.rsmod.game.hit.HitType.Melee, org.rsmod.game.hit.Hitmark(0), null, null, null)
        val ten = zero.copy(hitmark = zero.hitmark.copy(damage = 10))

        `when`(manager.queueMeleeHit(access, target, 0, 1)).thenReturn(zero)
        `when`(manager.queueMeleeHit(access, target, 10, 1)).thenReturn(ten)
        `when`(manager.queueMeleeHit(access, target, 10, 2)).thenReturn(ten)
        val five = zero.copy(hitmark = zero.hitmark.copy(damage = 5))

        `when`(manager.queueMeleeHit(access, target, 5, 1)).thenReturn(five)
        val item = InvObj(weapon)
        val attack = CombatAttack.Melee(item, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)
        val damage = mock(MeleeSpecialDamage::class.java)
        val random = mock(GameRandom::class.java)
        `when`(damage.maximum(player, target, attack, 110, 125, false)).thenReturn(10)
        `when`(damage.maximum(player, target, attack, 110, 110, false)).thenReturn(10)
        `when`(random.of(1, 10)).thenReturn(10)
        val special = register(MeleeWeaponSpecialAttacks(random, damage), manager)[item] as SpecialAttack.Melee
        return Fixture(manager, access, target, ten) { special.attack(access, target, attack) }
    }

    private class Fixture(
        val manager: SpecialAttackManager,
        val access: ProtectedAccess,
        val target: Npc,
        val queuedHit: Hit? = null,
        val action: suspend () -> Boolean,
    ) {
        fun run() {
            var completed = false
            action.startCoroutine(object : Continuation<Boolean> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(result: Result<Boolean>) { assertTrue(result.getOrThrow()); completed = true }
            })
            assertTrue(completed, "Special handler must queue work without suspending the player")
        }
    }
}
