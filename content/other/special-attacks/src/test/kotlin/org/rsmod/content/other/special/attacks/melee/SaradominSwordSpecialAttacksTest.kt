package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import java.util.EnumSet
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.api.combat.commons.*
import org.rsmod.api.combat.commons.styles.*
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.formulas.accuracy.magic.*
import org.rsmod.api.combat.formulas.accuracy.melee.*
import org.rsmod.api.combat.formulas.attributes.*
import org.rsmod.api.combat.formulas.attributes.collector.*
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.entity.*
import org.rsmod.game.hit.*
import org.rsmod.game.inv.InvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
class SaradominSwordSpecialAttacksTest {
    private var Player.protectMagic by intVarBit("varbit.prayer_protectfrommagic")

    @Test fun `ordinary sword keeps its one to sixteen magic hit separate and shares accuracy`() {
        for (accurate in listOf(false, true)) exercise("obj.saradomin_sword", accurate, npc())
    }

    @Test fun `Protect from Magic fully blocks only the ordinary swords secondary hit`() {
        exercise("obj.saradomin_sword", true, Player().apply { protectMagic = 1 })
    }

    @Test fun `both blessed identities queue melee damage with hybrid accuracy and native energy`() {
        for (weapon in listOf("obj.blessed_saradomin_sword", "obj.blessed_saradomin_sword_degraded")) {
            for (accurate in listOf(false, true)) {
                exercise(weapon, accurate, npc())
                exercise(weapon, accurate, Player().apply { protectMagic = 1 })
            }
        }
    }

    private fun exercise(weapon: String, accurate: Boolean, target: PathingEntity) {
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }.also { it.isAccessible = true }.invoke(weapons)
        val registry = SpecialAttackRegistry(weapons)
        val manager = mock(SpecialAttackManager::class.java)
        val accuracy = mock(MeleeAgainstMagicAccuracy::class.java)
        val damage = mock(MeleeSpecialDamage::class.java)
        val rng = mock(GameRandom::class.java)
        with(SaradominSwordSpecialAttacks(accuracy, damage, rng)) { SpecialAttackRepository(registry).register(manager) }
        val source = Player()
        val access = mock(ProtectedAccess::class.java)
        `when`(access.player).thenReturn(source)
        val attack = CombatAttack.Melee(InvObj(weapon), MeleeAttackType.Crush, MeleeAttackStyle.Aggressive, CombatStance.Stance3)
        val blessed = weapon != "obj.saradomin_sword"
        `when`(accuracy.roll(source, target, attack)).thenReturn(accurate)
        `when`(manager.rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Slash, 1.0)).thenReturn(accurate)
        `when`(manager.rollMeleeMaxHit(access, target, attack.type, attack.style, 1.1)).thenReturn(33)
        `when`(damage.maximum(source, target, attack, 125, 100, true)).thenReturn(50)
        `when`(rng.of(1, 50)).thenReturn(40)
        `when`(rng.of(1, 16)).thenReturn(16)
        val meleeAmount = if (!accurate) 0 else if (blessed) 40 else 33
        val magicAmount = if (accurate && !blessed && (target !is Player || target.protectMagic == 0)) 16 else 0
        `when`(damage.modifyRolledHit(source, target, attack, meleeAmount)).thenReturn(meleeAmount)
        if (target is Player) `when`(damage.reduce(target, magicAmount)).thenReturn(magicAmount)
        val meleeHit = Hit(HitType.Melee, Hitmark(0).copy(damage = meleeAmount), null, null, null)
        val magicHit = Hit(HitType.Magic, Hitmark(0).copy(damage = magicAmount), null, null, null)
        `when`(manager.queueMeleeHit(access, target, meleeAmount, 1)).thenReturn(meleeHit)
        `when`(manager.queueMagicHit(access, target, magicAmount, 30, 1, null)).thenReturn(magicHit)
        val special = registry[InvObj(weapon)] as SpecialAttack.Melee
        assertEquals(if (blessed) 650 else 1000, special.energyInHundreds)
        complete { special.attack(access, target, attack) }
        verify(manager).queueMeleeHit(access, target, meleeAmount, 1)
        verify(manager).giveCombatXp(access, target, attack, meleeAmount)
        if (blessed) {
            verify(accuracy).roll(source, target, attack)
            assertFalse(mockingDetails(manager).invocations.any { it.method.name == "queueMagicHit" })
        } else {
            verifyNoInteractions(accuracy)
            verify(manager).queueMagicHit(access, target, magicAmount, 30, 1, null)
            verify(manager).giveCombatXp(access, target, CombatAttack.Staff(requireNotNull(attack.weapon), MagicAttackStyle.Accurate), magicAmount)
        }
        val animation = if (blessed) SaradominSwordSpecialAttacks.BLESSED_ANIMATION else SaradominSwordSpecialAttacks.ANIMATION
        assertTrue(animation.asRSCM() >= 0)
        assertTrue(SaradominSwordSpecialAttacks.CASTER_EFFECT.asRSCM() >= 0)
        assertTrue(SaradominSwordSpecialAttacks.TARGET_EFFECT.asRSCM() >= 0)
        verify(access).anim(animation, 0)
        verify(access).spotanim(SaradominSwordSpecialAttacks.CASTER_EFFECT, 0, 0, constants.spotanim_slot_combat)
    }

    @Test fun `hybrid accuracy uses melee equipment with NPC magic levels or PvP magic defence`() {
        val melee = mock(PvNMeleeAccuracy::class.java)
        val npcMagic = mock(PvNMagicAccuracy::class.java)
        val playerMagic = mock(PvPMagicAccuracy::class.java)
        val meleeCollector = mock(CombatMeleeAttributeCollector::class.java)
        val npcCollector = mock(CombatNpcAttributeCollector::class.java)
        val rng = mock(GameRandom::class.java)
        val source = Player()
        val attack = CombatAttack.Melee(InvObj("obj.blessed_saradomin_sword"), MeleeAttackType.Crush, MeleeAttackStyle.Aggressive, CombatStance.Stance3)
        val flags = EnumSet.noneOf(CombatNpcAttributes::class.java)
        val equipment = EnumSet.noneOf(CombatMeleeAttributes::class.java)
        `when`(meleeCollector.collect(source, attack.type)).thenReturn(equipment)
        `when`(melee.computeAttackRoll(source, attack.type, attack.style, equipment, flags)).thenReturn(100)
        `when`(rng.of(10000)).thenReturn(0, 9999)
        val target = npc().apply { defenceLvl = 250; magicLvl = 50 }
        `when`(npcCollector.collect(target.visType, target, target.hitpoints, target.baseHitpointsLvl, false)).thenReturn(flags)
        `when`(npcMagic.computeDefenceRoll(target.visType, 250, 50, 0, flags, 0)).thenReturn(100)
        val playerTarget = Player()
        `when`(playerMagic.computeDefenceRoll(playerTarget)).thenReturn(100)
        val hybrid = MeleeAgainstMagicAccuracy(melee, npcMagic, playerMagic, meleeCollector, npcCollector, rng)
        assertTrue(hybrid.roll(source, target, attack))
        assertFalse(hybrid.roll(source, playerTarget, attack))
        verify(npcMagic).computeDefenceRoll(target.visType, 250, 50, 0, flags, 0)
        verify(playerMagic).computeDefenceRoll(playerTarget)
        verify(melee, times(2)).computeAttackRoll(source, attack.type, attack.style, equipment, flags)
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
