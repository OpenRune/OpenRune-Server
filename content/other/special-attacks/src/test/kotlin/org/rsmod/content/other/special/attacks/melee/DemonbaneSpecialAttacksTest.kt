package org.rsmod.content.other.special.attacks.melee

import dev.openrune.ServerCacheManager
import kotlin.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.parallel.*
import org.mockito.Mockito.*
import org.rsmod.annotations.InternalApi
import org.rsmod.api.combat.commons.*
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.*
import org.rsmod.api.specials.weapon.SpecialAttackWeapons
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.*
import org.rsmod.game.inv.InvObj

@ResourceLock("ServerCacheManager")
@Execution(ExecutionMode.SAME_THREAD)
@OptIn(InternalApi::class)
class DemonbaneSpecialAttacksTest {
    @Test fun `drains use base levels additively and clamp at zero`() {
        for ((demon, emberlight, drain) in listOf(Triple(false, false, 6), Triple(false, true, 6),
            Triple(true, false, 11), Triple(true, true, 16))) {
            val target = npc(demon).apply {
                baseAttackLvl = 100; baseStrengthLvl = 100; baseDefenceLvl = 100
                attackLvl = 50; strengthLvl = 50; defenceLvl = 50
            }
            repeat(2) { DemonbaneSpecialAttacks.weaken(target, emberlight) }
            assertEquals(listOf(50 - 2 * drain, 50 - 2 * drain, 50 - 2 * drain),
                listOf(target.attackLvl, target.strengthLvl, target.defenceLvl))
            repeat(20) { DemonbaneSpecialAttacks.weaken(target, emberlight) }
            assertEquals(listOf(0, 0, 0), listOf(target.attackLvl, target.strengthLvl, target.defenceLvl))
        }
    }

    @Test fun `accurate hit drains on impact once even if final damage is zero`() {
        val source = Player().apply { slotId = 1; uuid = 1; assignUid() }
        val target = npc(false).apply { slotId = 2; assignUid(); baseDefenceLvl = 100; defenceLvl = 100 }
        val hit = Hit(HitType.Melee, Hitmark(0), null, null, null)
        DemonbaneSpecialAttacks.attachWeaken(hit, source, target, false)
        assertEquals(100, target.defenceLvl)
        hit.impactEffects.complete(0)
        hit.impactEffects.complete(0)
        assertEquals(94, target.defenceLvl)
        val cancelled = hit.copy(impactEffects = HitImpactEffects())
        DemonbaneSpecialAttacks.attachWeaken(cancelled, source, target, true)
        source.uuid = 2; source.assignUid()
        cancelled.impactEffects.complete(10)
        assertEquals(94, target.defenceLvl)
    }

    @Test fun `all variants use stab defence caster effects and never drain on an accuracy miss`() {
        val weapons = SpecialAttackWeapons()
        SpecialAttackWeapons::class.java.declaredMethods.single { it.name.startsWith("startup") }
            .also { it.isAccessible = true }.invoke(weapons)
        for (weapon in listOf("obj.darklight", "obj.arclight", "obj.arclight_inactive", "obj.emberlight")) {
            for (accurate in listOf(false, true)) {
                val manager = mock(SpecialAttackManager::class.java)
                val registry = SpecialAttackRegistry(weapons)
                with(DemonbaneSpecialAttacks()) { SpecialAttackRepository(registry).register(manager) }
                val access = mock(ProtectedAccess::class.java)
                val source = Player().apply { slotId = 1; uuid = 1; assignUid() }
                `when`(access.player).thenReturn(source)
                val target = npc(false).apply { slotId = 2; assignUid(); baseDefenceLvl = 100; defenceLvl = 100 }
                val attack = CombatAttack.Melee(InvObj(weapon), MeleeAttackType.Slash, MeleeAttackStyle.Accurate, CombatStance.Stance1)
                `when`(manager.rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Stab, 1.0)).thenReturn(accurate)
                `when`(manager.rollMeleeMaxHit(access, target, attack.type, attack.style, 1.0)).thenReturn(10)
                val amount = if (accurate) 10 else 0
                val hit = Hit(HitType.Melee, Hitmark(0).copy(damage = amount), null, null, null)
                `when`(manager.queueMeleeHit(access, target, amount, 1)).thenReturn(hit)
                val special = registry[requireNotNull(attack.weapon)] as SpecialAttack.Melee
                assertEquals(500, special.energyInHundreds)
                var completed = false
                suspend { special.attack(access, target, attack) }.startCoroutine(object : Continuation<Boolean> {
                    override val context = EmptyCoroutineContext
                    override fun resumeWith(result: Result<Boolean>) { assertTrue(result.getOrThrow()); completed = true }
                })
                assertTrue(completed)
                val emberlight = weapon == "obj.emberlight"
                verify(access).anim(if (emberlight) "seq.human_weapon_emberlight_01_spec" else "seq.dark_spec_player", 0)
                verify(access).spotanim(if (emberlight) "spotanim.vfx_emberlight_spec_02" else "spotanim.dark_spec_spot", 0, 0, constants.spotanim_slot_combat)
                verify(manager).rollMeleeAccuracy(access, target, attack.type, attack.style, MeleeAttackType.Stab, 1.0)
                assertEquals(100, target.defenceLvl)
                hit.impactEffects.complete(amount)
                assertEquals(if (accurate) 94 else 100, target.defenceLvl)
            }
        }
    }

    private fun npc(demon: Boolean): Npc {
        val base = ServerCacheManager.getNpcs().values.first { it.name == "Goblin" }
        val type = base.copy(paramsRaw = base.paramsRaw.orEmpty().toMutableMap().apply {
            put(params.demon.id, if (demon) 1 else 0)
        })
        type.paramMap = dev.openrune.ParamMap(checkNotNull(type.paramsRaw))
        return Npc(type)
    }

    companion object { @JvmStatic @BeforeAll fun setup() { ServerCacheManager.init(240).close() } }
}
