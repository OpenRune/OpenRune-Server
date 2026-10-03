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
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.*
import org.rsmod.game.inv.InvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class DragonClawsSpecialAttackTest {
    @Test fun `all four accuracy branches stop rolling at first success`() {
        val expected = listOf(intArrayOf(39, 19, 9, 10), intArrayOf(0, 34, 17, 18), intArrayOf(0, 0, 29, 30), intArrayOf(0, 0, 0, 50))
        for (first in 0..3) {
            var rolls = 0
            val random = mock(GameRandom::class.java)
            assertArrayEquals(expected[first], DragonClawsSpecialAttack.roll(40, random, true) { rolls++ == first })
            assertEquals(first + 1, rolls)
            verifyNoInteractions(random)
        }
    }

    @Test fun `all misses have one-third zero and two-thirds chip outcomes`() {
        for (fallback in 0..2) {
            val random = mock(GameRandom::class.java)
            `when`(random.of(3)).thenReturn(fallback)
            var rolls = 0
            assertArrayEquals(if (fallback == 0) intArrayOf(0, 0, 0, 0) else intArrayOf(0, 0, 1, 1),
                DragonClawsSpecialAttack.roll(40, random) { rolls++; false })
            assertEquals(4, rolls)
            verify(random).of(3)
        }
    }

    @Test fun `damage roll bounds and rounding match each successful accuracy branch`() {
        val ranges = listOf(40 to 79, 30 to 69, 20 to 59, 10 to 49)
        for ((first, range) in ranges.withIndex()) {
            val random = mock(GameRandom::class.java)
            `when`(random.of(range.first, range.second)).thenReturn(range.first)
            var index = 0
            val hits = DragonClawsSpecialAttack.roll(40, random) { index++ == first }
            verify(random).of(range.first, range.second)
            assertEquals(4, hits.size)
            assertTrue(hits.all { it >= 0 })
        }
    }

    @Test fun `every variant queues paired hits with caster animation and effect`() {
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }.also { it.isAccessible = true }.invoke(weapons)
        assertTrue(DragonClawsSpecialAttack.ANIMATION.asRSCM() >= 0)
        assertTrue(DragonClawsSpecialAttack.EFFECT.asRSCM() >= 0)
        for (symbol in DragonClawsSpecialAttack.WEAPONS) {
            val manager = mock(SpecialAttackManager::class.java)
            val random = mock(GameRandom::class.java)
            val registry = SpecialAttackRegistry(weapons)
            with(DragonClawsSpecialAttack(random)) { SpecialAttackRepository(registry).register(manager) }
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(Player())
            val target = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
            val attack = CombatAttack.Melee(InvObj(symbol), MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)
            `when`(manager.calculateMeleeMaxHit(access, target, attack.type, attack.style, 1.0)).thenReturn(40)
            `when`(manager.rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Slash, 1.0)).thenReturn(true)
            `when`(random.of(40, 79)).thenReturn(79)
            for ((amount, delay) in listOf(39 to 1, 19 to 1, 9 to 2, 10 to 2)) {
                `when`(manager.queueMeleeHit(access, target, amount, delay)).thenReturn(Hit(HitType.Melee, Hitmark(0).copy(damage = amount), null, null, null))
            }
            val special = registry[InvObj(symbol)] as SpecialAttack.Melee
            assertEquals(500, special.energyInHundreds)
            var result: Boolean? = null
            suspend { special.attack(access, target, attack) }.startCoroutine(object : Continuation<Boolean> {
                override val context = EmptyCoroutineContext
                override fun resumeWith(value: Result<Boolean>) { result = value.getOrThrow() }
            })
            assertEquals(true, result)
            verify(access).anim(DragonClawsSpecialAttack.ANIMATION, 0)
            verify(access).spotanim(DragonClawsSpecialAttack.EFFECT, 0, 0, constants.spotanim_slot_combat)
            for ((amount, delay) in listOf(39 to 1, 19 to 1, 9 to 2, 10 to 2)) verify(manager).queueMeleeHit(access, target, amount, delay)
            verify(manager, times(1)).rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Slash, 1.0)
        }
    }

    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
