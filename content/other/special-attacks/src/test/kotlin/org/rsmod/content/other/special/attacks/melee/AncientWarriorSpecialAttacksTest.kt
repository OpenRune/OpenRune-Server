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
class AncientWarriorSpecialAttacksTest {
    @Test fun `all seven identities route accuracy damage ranges energy and effects`() {
        val identities = AncientWarriorSpecialAttacks.LONGSWORDS + AncientWarriorSpecialAttacks.HAMMERS + "obj.statius_warhammer_bh"
        for (weapon in identities) for (accurate in listOf(false, true)) for (playerTarget in listOf(false, true)) {
            val longsword = weapon in AncientWarriorSpecialAttacks.LONGSWORDS
            val manager = mock(SpecialAttackManager::class.java)
            val accuracy = mock(ReducedMeleeDefenceAccuracy::class.java)
            val damage = mock(MeleeSpecialDamage::class.java)
            val rng = mock(GameRandom::class.java)
            val weapons = SpecialAttackWeapons()
            SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }
                .also { it.isAccessible = true }.invoke(weapons)
            val registry = SpecialAttackRegistry(weapons)
            with(AncientWarriorSpecialAttacks(accuracy, damage, rng)) { SpecialAttackRepository(registry).register(manager) }
            val source = Player().apply { slotId = 1; uuid = 1; assignUid() }
            val target: PathingEntity = if (playerTarget) Player() else npc().apply { slotId = 2; assignUid(); defenceLvl = 100 }
            val access = mock(ProtectedAccess::class.java)
            `when`(access.player).thenReturn(source)
            val attack = CombatAttack.Melee(InvObj(weapon), MeleeAttackType.Slash, MeleeAttackStyle.Aggressive, CombatStance.Stance2)
            `when`(accuracy.roll(source, target, attack, MeleeAttackType.Stab, 25)).thenReturn(accurate)
            `when`(manager.rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Crush, 1.0)).thenReturn(accurate)
            `when`(damage.maximum(source, target, attack, 100, 100, true)).thenReturn(40)
            val minimum = if (longsword) 8 else 10
            val maximum = if (longsword) 48 else 50
            `when`(rng.of(minimum, maximum)).thenReturn(maximum)
            val rolled = if (accurate) maximum else 0
            val amount = rolled / 2
            `when`(damage.modifyRolledHit(source, target, attack, rolled)).thenReturn(amount)
            val hit = hit(amount)
            `when`(manager.queueMeleeHit(access, target, amount, 1)).thenReturn(hit)
            val special = registry[InvObj(weapon)] as SpecialAttack.Melee
            assertEquals(if (longsword) 250 else 350, special.energyInHundreds)
            complete { special.attack(access, target, attack) }
            verify(manager).queueMeleeHit(access, target, amount, 1)
            verify(manager).giveCombatXp(access, target, attack, amount)
            verify(damage).modifyRolledHit(source, target, attack, rolled)
            if (accurate) verify(rng).of(minimum, maximum) else verifyNoInteractions(rng)
            if (longsword) {
                verify(accuracy).roll(source, target, attack, MeleeAttackType.Stab, 25)
                verify(access).anim(AncientWarriorSpecialAttacks.LONGSWORD_ANIMATION, 0)
            } else {
                verifyNoInteractions(accuracy)
                verify(access).anim(AncientWarriorSpecialAttacks.HAMMER_ANIMATION, 0)
                verify(access).spotanim(AncientWarriorSpecialAttacks.HAMMER_EFFECT, 0, 0, constants.spotanim_slot_combat)
            }
            if (target is Npc) {
                assertEquals(100, target.defenceLvl)
                hit.impactEffects.complete(amount)
                val drain = if (weapon == "obj.statius_warhammer_bh") 75 else 30
                assertEquals(if (accurate && !longsword) 100 - drain else 100, target.defenceLvl)
            }
        }
        listOf(AncientWarriorSpecialAttacks.LONGSWORD_ANIMATION, AncientWarriorSpecialAttacks.HAMMER_ANIMATION,
            AncientWarriorSpecialAttacks.HAMMER_EFFECT).forEach { assertTrue(it.asRSCM() >= 0) }
    }

    @Test fun `defence drain waits for impact stacks from current level and rejects old logins`() {
        val source = Player().apply { slotId = 1; uuid = 1; assignUid() }
        val target = npc().apply { slotId = 2; assignUid(); defenceLvl = 101 }
        repeat(2) {
            val hit = hit(20)
            AncientWarriorSpecialAttacks.attachDefenceDrain(hit, source, target, 75)
            hit.impactEffects.complete(1)
            hit.impactEffects.complete(1)
        }
        assertEquals(7, target.defenceLvl)
        val blocked = hit(20)
        AncientWarriorSpecialAttacks.attachDefenceDrain(blocked, source, target, 75)
        blocked.impactEffects.complete(0)
        assertEquals(7, target.defenceLvl)
        val stale = hit(20)
        AncientWarriorSpecialAttacks.attachDefenceDrain(stale, source, target, 75)
        source.uuid = 2; source.assignUid()
        stale.impactEffects.complete(20)
        assertEquals(7, target.defenceLvl)
    }

    @Test fun `Feint quarters the defence roll without mutating NPC levels or multiplying offence`() {
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
        val attack = CombatAttack.Melee(InvObj("obj.vestas_longsword"), MeleeAttackType.Slash, MeleeAttackStyle.Accurate, CombatStance.Stance1)
        `when`(meleeCollector.collect(source, attack.type)).thenReturn(equipment)
        `when`(npcCollector.collect(npc.visType, npc, npc.hitpoints, npc.baseHitpointsLvl, false)).thenReturn(flags)
        `when`(npcMelee.computeAttackRoll(source, attack.type, attack.style, equipment, flags)).thenReturn(100)
        `when`(npcMelee.computeDefenceRoll(npc.visType, 101, 0, MeleeAttackType.Stab, flags)).thenReturn(400)
        `when`(playerMelee.computeDefenceRoll(targetPlayer, MeleeAttackType.Stab)).thenReturn(400)
        val helper = ReducedMeleeDefenceAccuracy(npcMelee, playerMelee, meleeCollector, npcCollector, rng)
        // Equal rolls of 100 give a threshold of 4950, not 5000 or four times the old probability.
        `when`(rng.of(10000)).thenReturn(4949, 4950)
        assertTrue(helper.roll(source, npc, attack, MeleeAttackType.Stab, 25))
        assertFalse(helper.roll(source, targetPlayer, attack, MeleeAttackType.Stab, 25))
        assertEquals(101, npc.defenceLvl)
        verify(npcMelee).computeDefenceRoll(npc.visType, 101, 0, MeleeAttackType.Stab, flags)
        verify(playerMelee).computeDefenceRoll(targetPlayer, MeleeAttackType.Stab)
    }

    private fun npc() = Npc(ServerCacheManager.getNpcs().values.first { it.name == "Goblin" })
    private fun hit(amount: Int) = Hit(HitType.Melee, Hitmark(0).copy(damage = amount), null, null, null)
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
