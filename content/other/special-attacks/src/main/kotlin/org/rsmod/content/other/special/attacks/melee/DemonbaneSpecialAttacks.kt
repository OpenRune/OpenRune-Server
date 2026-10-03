package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.config.refs.params
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statSub
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player
import org.rsmod.game.hit.Hit

class DemonbaneSpecialAttacks : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        for (weapon in listOf("obj.darklight", "obj.arclight", "obj.arclight_inactive")) {
            registerMelee(weapon, Weaken(manager, false))
        }
        registerMelee("obj.emberlight", Weaken(manager, true))
    }

    private class Weaken(private val manager: SpecialAttackManager, private val emberlight: Boolean) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)

        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            anim(if (emberlight) "seq.human_weapon_emberlight_01_spec" else "seq.dark_spec_player")
            spotanim(if (emberlight) "spotanim.vfx_emberlight_spec_02" else "spotanim.dark_spec_spot",
                height = 0, slot = constants.spotanim_slot_combat)
            val accurate = manager.rollMeleeAccuracy(this, target, attack.type, attack.style, MeleeAttackType.Stab, 1.0)
            val damage = if (accurate) manager.rollMeleeMaxHit(this, target, attack.type, attack.style, 1.0).coerceAtLeast(1) else 0
            val hit = manager.queueMeleeHit(this, target, damage, 1)
            manager.giveCombatXp(this, target, attack, hit.damage)
            if (accurate) attachWeaken(hit, player, target, emberlight)
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        fun attachWeaken(hit: Hit, source: Player, target: PathingEntity, emberlight: Boolean) {
            val sourceUid = source.uid.packed
            val targetUid = when (target) { is Npc -> target.uid.packed; is Player -> target.uid.packed }
            hit.impactEffects.add {
                val sameTarget = when (target) {
                    is Npc -> target.isSlotAssigned && target.uid.packed == targetUid
                    is Player -> target.isSlotAssigned && target.uid.packed == targetUid
                }
                if (sameTarget && source.isSlotAssigned && source.uid.packed == sourceUid) weaken(target, emberlight)
            }
        }

        fun weaken(target: PathingEntity, emberlight: Boolean) {
            val demon = target is Npc && (target.visType.paramOrNull(params.demon) ?: 0) != 0
            val percent = if (!demon) 5 else if (emberlight) 15 else 10
            when (target) {
                is Npc -> {
                    target.attackLvl = reduced(target.attackLvl, target.baseAttackLvl, percent)
                    target.strengthLvl = reduced(target.strengthLvl, target.baseStrengthLvl, percent)
                    target.defenceLvl = reduced(target.defenceLvl, target.baseDefenceLvl, percent)
                }
                is Player -> for (stat in listOf("stat.attack", "stat.strength", "stat.defence")) {
                    target.statSub(stat, target.statBase(stat) * percent / 100 + 1, 0)
                }
            }
        }

        private fun reduced(current: Int, base: Int, percent: Int): Int =
            (current - (base * percent / 100 + 1)).coerceAtLeast(0)
    }
}
