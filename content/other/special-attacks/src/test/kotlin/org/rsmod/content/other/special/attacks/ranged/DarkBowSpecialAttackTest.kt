package org.rsmod.content.other.special.attacks.ranged

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.util.Wearpos
import java.util.EnumSet
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.styles.RangedAttackStyle
import org.rsmod.api.combat.commons.types.RangedAttackType
import org.rsmod.api.combat.formulas.attributes.*
import org.rsmod.api.combat.formulas.attributes.collector.*
import org.rsmod.api.combat.formulas.maxhit.ranged.*
import org.rsmod.api.combat.manager.RangedAmmoManager
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.entity.*
import org.rsmod.game.hit.*
import org.rsmod.game.inv.*
import org.rsmod.game.proj.ProjAnim
import org.rsmod.game.type.getInvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class DarkBowSpecialAttackTest {
    @Test fun `misses retain minimum and ordinary bow clamps rather than rerolls low damage`() {
        for (minimum in listOf(5, 8, 7, 10)) {
            assertEquals(minimum, DarkBowSpecialAttack.rollDamage(false, 60, minimum, 48, false) { fail("miss must not roll") })
        }
        assertEquals(8, DarkBowSpecialAttack.rollDamage(true, 60, 8, 48, false) { assertEquals(0..60, it); 0 })
        assertEquals(48, DarkBowSpecialAttack.rollDamage(true, 60, 8, 48, false) { 60 })
        assertEquals(8, DarkBowSpecialAttack.rollDamage(true, 0, 8, 48, false) { 0 })
    }

    @Test fun `BH rolls uniformly within its higher minimum and cap`() {
        assertEquals(10, DarkBowSpecialAttack.rollDamage(true, 60, 10, 48, true) { assertEquals(10..48, it); it.first })
        assertEquals(7, DarkBowSpecialAttack.rollDamage(true, 3, 7, Int.MAX_VALUE, true) { assertEquals(7..7, it); it.last })
    }

    @Test fun `native Corp reduction is applied after the guaranteed minimum`() {
        val source = Player()
        val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
        val ranged = mock(CombatRangedAttributeCollector::class.java)
        val npcs = mock(CombatNpcAttributeCollector::class.java)
        val speeds = mock(WeaponSpeeds::class.java)
        val bonuses = mock(WornBonuses::class.java)
        val attack = CombatAttack.Ranged(InvObj("obj.darkbow"), RangedAttackType.Standard, RangedAttackStyle.Accurate)
        `when`(ranged.collect(source, attack.type, attack.style)).thenReturn(EnumSet.noneOf(CombatRangedAttributes::class.java))
        `when`(npcs.collect(target.visType, target, target.hitpoints, target.baseHitpointsLvl, false)).thenReturn(EnumSet.of(CombatNpcAttributes.CorporealBeast))
        val formula = RangedSpecialDamage(ranged, npcs, PvNRangedMaxHit(bonuses, speeds, npcs, ranged),
            mock(PvPRangedMaxHit::class.java), mock(DamageReductionAttributeCollector::class.java), mock(GameRandom::class.java))
        val minimum = DarkBowSpecialAttack.rollDamage(false, 60, 8, 48, false) { fail("miss must not roll") }
        assertEquals(4, formula.modifyRolledHit(source, target, attack, minimum))
        assertEquals(24, formula.modifyRolledHit(source, target, attack, 48))
    }

    @Test fun `all nine bows fire two arrows with matching projectile delays and minimum damage`() {
        for (weapon in listOf("obj.darkbow", "obj.darkbow_green", "obj.darkbow_blue", "obj.darkbow_yellow", "obj.darkbow_white", "obj.br_darkbow", "obj.deadman_blighted_dark_bow", "obj.deadman_darkbow", "obj.bh_darkbow_imbue")) {
            exercise(weapon, "obj.rune_arrow", 2)
            exercise(weapon, "obj.dragon_arrow", 2)
        }
    }

    @Test fun `one arrow rejects the attack before effects consumption or damage`() {
        exercise("obj.darkbow", "obj.dragon_arrow", 1)
    }

    @Test fun `missing seeking arrow double launch metadata rejects without consuming energy or ammo`() {
        exercise("obj.darkbow", "obj.seeking_dragon_arrow", 2, expectedSuccess = false)
    }

    private fun exercise(symbol: String, ammoSymbol: String, count: Int, expectedSuccess: Boolean = count >= 2) {
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }.also { it.isAccessible = true }.invoke(weapons)
        val manager = mock(SpecialAttackManager::class.java)
        val ammo = mock(RangedAmmoManager::class.java)
        val formula = mock(RangedSpecialDamage::class.java)
        val access = mock(ProtectedAccess::class.java)
        val player = Player().apply { worn = Inventory(checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())), arrayOfNulls(14)) }
        val item = InvObj(symbol)
        val arrows = InvObj(ammoSymbol, count)
        player.worn[Wearpos.RightHand.slot] = item
        player.worn[Wearpos.Quiver.slot] = arrows
        `when`(access.player).thenReturn(player)
        val target = spy(Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }))
        val arrowType = getInvObj(arrows)
        `when`(ammo.attemptAmmoUsage(player, getInvObj(item), arrowType)).thenReturn(true)
        val attack = CombatAttack.Ranged(item, RangedAttackType.Standard, RangedAttackStyle.Accurate)
        val dragon = ammoSymbol == "obj.dragon_arrow" || ammoSymbol == "obj.seeking_dragon_arrow"
        val minimum = (if (dragon) 8 else 5) + if (symbol == "obj.bh_darkbow_imbue") 2 else 0
        `when`(formula.maximum(player, target, attack, if (dragon) 1.5 else 1.3)).thenReturn(60)
        `when`(formula.modifyRolledHit(player, target, attack, minimum)).thenReturn(minimum)
        val travel = RSCM.getReverseMapping(RSCMType.SPOTANIM, arrowType.param(params.proj_travel).id)
        for ((path, cycles) in listOf("projanim.doublearrow_one" to 30, "projanim.doublearrow_two" to 60)) {
            val proj = mock(ProjAnim::class.java)
            `when`(proj.clientCycles).thenReturn(cycles)
            `when`(proj.serverCycles).thenReturn(1 + cycles / 30)
            `when`(manager.spawnProjectile(access, target, travel, path)).thenReturn(proj)
        }
        val hit = Hit(HitType.Ranged, Hitmark(0).copy(damage = minimum), null, null, null)
        `when`(manager.queueRangedHit(access, target, arrowType, minimum, 30, 2)).thenReturn(hit)
        `when`(manager.queueRangedDamage(access, target, arrowType, minimum, 3)).thenReturn(hit)
        val registry = SpecialAttackRegistry(weapons)
        with(DarkBowSpecialAttack(ammo, formula)) { SpecialAttackRepository(registry).register(manager) }
        val special = registry[item] as SpecialAttack.Ranged
        assertEquals(550, special.energyInHundreds)
        var result: Result<Boolean>? = null
        suspend { special.attack(access, target, attack) }.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(value: Result<Boolean>) { result = value }
        })
        assertEquals(expectedSuccess, checkNotNull(result).getOrThrow(), "$symbol with $ammoSymbol")
        if (!expectedSuccess) {
            verifyNoInteractions(formula)
            assertFalse(mockingDetails(access).invocations.any { it.method.name == "anim" })
            assertFalse(mockingDetails(manager).invocations.any { it.method.name.startsWith("queueRanged") || it.method.name == "spawnProjectile" })
            assertFalse(mockingDetails(ammo).invocations.any { it.method.name.startsWith("useQuiverAmmo") })
            return
        }
        verify(access).anim("seq.human_bow", 0)
        verify(access).spotanim(RSCM.getReverseMapping(RSCMType.SPOTANIM, arrowType.param(params.proj_launch_double).id), 0, 96, constants.spotanim_slot_combat)
        val impact = if (dragon) "spotanim.darkbow_dragon_head_flying_impact_anim" else "spotanim.darkbow_smoke_arrow_impact"
        verify(target).spotanim(impact, 60, 96, 0)
        verify(manager).queueRangedHit(access, target, arrowType, minimum, 30, 2)
        verify(manager).queueRangedDamage(access, target, arrowType, minimum, 3)
        verify(manager, times(2)).giveCombatXp(access, target, attack, minimum)
        assertEquals(2, mockingDetails(ammo).invocations.count { it.method.name.startsWith("useQuiverAmmo") })
    }

    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
