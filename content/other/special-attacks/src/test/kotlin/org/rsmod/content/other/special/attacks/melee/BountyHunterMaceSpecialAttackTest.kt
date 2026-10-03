package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.util.EnumSet
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.*
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.accuracy.melee.*
import org.rsmod.api.combat.formulas.attributes.*
import org.rsmod.api.combat.formulas.attributes.collector.*
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.entity.*
import org.rsmod.game.hit.*
import org.rsmod.game.inv.InvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class BountyHunterMaceSpecialAttackTest {
    @Test fun `Shatter routes selected offence to reduced crush defence and applies damage reduction after roll`() {
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }.also { it.isAccessible = true }.invoke(weapons)
        for (target in listOf<PathingEntity>(npc(), Player())) for (accurate in listOf(false, true)) {
            val registry = SpecialAttackRegistry(weapons)
            val manager = mock(SpecialAttackManager::class.java)
            val accuracy = mock(ReducedMeleeDefenceAccuracy::class.java)
            val formula = mock(MeleeSpecialDamage::class.java)
            val rng = mock(GameRandom::class.java)
            with(BountyHunterMaceSpecialAttack(accuracy, formula, rng)) { SpecialAttackRepository(registry).register(manager) }
            val source = Player()
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(source)
            val item = InvObj("obj.bh_dragon_mace_imbue")
            val attack = CombatAttack.Melee(item, MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)
            `when`(accuracy.roll(source, target, attack, MeleeAttackType.Crush, 60, 125)).thenReturn(accurate)
            `when`(formula.maximum(source, target, attack, 150, 100, true)).thenReturn(60)
            `when`(rng.of(1, 60)).thenReturn(40)
            `when`(formula.modifyRolledHit(source, target, attack, 40)).thenReturn(20)
            val amount = if (accurate) 20 else 0
            val hit = Hit(HitType.Melee, Hitmark(0).copy(damage = amount), null, null, null)
            `when`(manager.queueMeleeHit(access, target, amount, 1)).thenReturn(hit)
            val special = registry[item] as SpecialAttack.Melee
            assertEquals(150, special.energyInHundreds)
            complete { special.attack(access, target, attack) }
            verify(accuracy).roll(source, target, attack, MeleeAttackType.Crush, 60, 125)
            verify(manager).queueMeleeHit(access, target, amount, 1)
            verify(manager).giveCombatXp(access, target, attack, amount)
            verify(access).anim("seq.shatter", 0)
            verify(access).spotanim("spotanim.sp_attack_shatter_spotanim", 0, 0, constants.spotanim_slot_combat)
            if (accurate) verify(formula).modifyRolledHit(source, target, attack, 40) else verifyNoInteractions(formula, rng)
        }
        assertTrue("seq.shatter".asRSCM() >= 0)
        assertTrue("spotanim.sp_attack_shatter_spotanim".asRSCM() >= 0)
    }

    @Test fun `Shatter combines 125 percent offence with 60 percent defence without changing target stats`() {
        val npcMelee = mock(PvNMeleeAccuracy::class.java)
        val playerMelee = mock(PvPMeleeAccuracy::class.java)
        val meleeCollector = mock(CombatMeleeAttributeCollector::class.java)
        val npcCollector = mock(CombatNpcAttributeCollector::class.java)
        val rng = mock(GameRandom::class.java)
        val source = Player()
        val npc = npc().apply { defenceLvl = 101 }
        val targetPlayer = Player()
        val flags = EnumSet.noneOf(CombatNpcAttributes::class.java)
        val equipment = EnumSet.noneOf(CombatMeleeAttributes::class.java)
        val attack = CombatAttack.Melee(InvObj("obj.bh_dragon_mace_imbue"), MeleeAttackType.Stab, MeleeAttackStyle.Accurate, CombatStance.Stance1)
        `when`(meleeCollector.collect(source, attack.type)).thenReturn(equipment)
        `when`(npcCollector.collect(npc.visType, npc, npc.hitpoints, npc.baseHitpointsLvl, false)).thenReturn(flags)
        `when`(npcMelee.computeAttackRoll(source, attack.type, attack.style, equipment, flags)).thenReturn(100)
        `when`(npcMelee.computeDefenceRoll(npc.visType, 101, 0, MeleeAttackType.Crush, flags)).thenReturn(400)
        `when`(playerMelee.computeDefenceRoll(targetPlayer, MeleeAttackType.Crush)).thenReturn(400)
        val helper = ReducedMeleeDefenceAccuracy(npcMelee, playerMelee, meleeCollector, npcCollector, rng)
        // 125 / (2 * (240 + 1)) = 2593/10000 with the native accuracy rounding.
        `when`(rng.of(10000)).thenReturn(2592, 2593)
        assertTrue(helper.roll(source, npc, attack, MeleeAttackType.Crush, 60, 125))
        assertFalse(helper.roll(source, targetPlayer, attack, MeleeAttackType.Crush, 60, 125))
        assertEquals(101, npc.defenceLvl)
    }

    private fun npc() = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
    private fun complete(block: suspend () -> Boolean) {
        var done = false
        block.startCoroutine(object : Continuation<Boolean> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<Boolean>) { assertTrue(result.getOrThrow()); done = true }
        })
        assertTrue(done)
    }
    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
