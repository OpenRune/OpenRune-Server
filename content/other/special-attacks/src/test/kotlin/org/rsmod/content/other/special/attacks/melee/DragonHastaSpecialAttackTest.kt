package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.*
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.specials.*
import org.rsmod.api.specials.energy.SpecialAttackEnergy
import org.rsmod.api.specials.energy.SpecialAttackEnergyModifier
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.entity.*
import org.rsmod.game.hit.*
import org.rsmod.game.inv.InvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class DragonHastaSpecialAttackTest {
    private var Player.energy by intVarp("varp.sa_energy")

    @Test fun `all variants spend the bar scale partial energy and use the distinct hasta effect`() {
        assertTrue(DragonHastaSpecialAttack.ANIMATION.asRSCM() >= 0)
        assertTrue(DragonHastaSpecialAttack.EFFECT.asRSCM() >= 0)
        assertNotEquals("spotanim.dragon_sword_spec_spotanim".asRSCM(), DragonHastaSpecialAttack.EFFECT.asRSCM())
        for (weapon in DragonHastaSpecialAttack.WEAPONS) {
            for (energy in listOf(50, 250, 1000)) {
                for (accurate in listOf(false, true)) {
                    exercise(weapon, energy, accurate, playerTarget = false)
                    exercise(weapon, energy, accurate, playerTarget = true)
                }
            }
        }
    }

    @Test fun `extra cost uses the same native energy modifier as the minimum cost`() {
        exercise(DragonHastaSpecialAttack.WEAPONS.first(), 250, true, false, divisor = 2)
    }

    private fun exercise(weapon: String, energy: Int, accurate: Boolean, playerTarget: Boolean, divisor: Int = 1) {
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }
            .also { it.isAccessible = true }.invoke(weapons)
        val registry = SpecialAttackRegistry(weapons)
        val manager = mock(SpecialAttackManager::class.java)
        with(DragonHastaSpecialAttack()) { SpecialAttackRepository(registry).register(manager) }
        val source = Player().also { it.energy = energy }
        if (divisor != 1) SpecialAttackEnergyModifier.setCostDivisor(source, divisor)
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(source)
        val target: PathingEntity = if (playerTarget) Player() else Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
        val attack = CombatAttack.Melee(InvObj(weapon), MeleeAttackType.Slash, MeleeAttackStyle.Accurate, CombatStance.Stance1)
        val steps = energy / 50
        `when`(manager.rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Stab, 1.0 + steps * 0.05)).thenReturn(accurate)
        `when`(manager.rollMeleeMaxHit(access, target, attack.type, attack.style, 1.0 + steps * 0.025)).thenReturn(30)
        val damage = if (accurate) 30 else 0
        val hit = Hit(HitType.Melee, Hitmark(0).copy(damage = damage), null, null, null)
        `when`(manager.queueMeleeHitIgnoringPrayer(access, target, damage, 1)).thenReturn(hit)
        val nativeEnergy = SpecialAttackEnergy()
        val extra = (energy - 50).coerceAtLeast(0)
        if (extra > 0) doAnswer { nativeEnergy.takeSpecialEnergy(source, extra); null }
            .`when`(manager).takeSpecialEnergy(access, extra)
        val special = registry[InvObj(weapon)] as SpecialAttack.Melee
        assertEquals(50, special.energyInHundreds)
        assertTrue(nativeEnergy.hasSpecialEnergy(source, special.energyInHundreds))
        var completed = false
        suspend { special.attack(access, target, attack) }.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { assertTrue(result.getOrThrow()); completed = true }
        })
        assertTrue(completed)
        // Same final deduction as PlayerCommons, after the handler's successful return.
        nativeEnergy.takeSpecialEnergy(source, special.energyInHundreds)
        assertEquals(if (divisor == 1) 0 else 125, source.energy)
        verify(access).anim(DragonHastaSpecialAttack.ANIMATION, 0)
        verify(access).spotanim(DragonHastaSpecialAttack.EFFECT, 0, 0, constants.spotanim_slot_combat)
        verify(manager).rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Stab, 1.0 + steps * 0.05)
        verify(manager).queueMeleeHitIgnoringPrayer(access, target, damage, 1)
        verify(manager).giveCombatXp(access, target, attack, damage)
        if (accurate) verify(manager).rollMeleeMaxHit(access, target, attack.type, attack.style, 1.0 + steps * 0.025)
        if (extra > 0) verify(manager).takeSpecialEnergy(access, extra)
    }

    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
