package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

class DragonHastaSpecialAttack @Inject constructor() : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val handler = Unleash(manager)
        WEAPONS.forEach { registerMelee(it, handler) }
    }

    private class Unleash(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(target: Npc, attack: CombatAttack.Melee): Boolean = perform(target, attack)
        override suspend fun ProtectedAccess.attack(target: Player, attack: CombatAttack.Melee): Boolean = perform(target, attack)

        private fun ProtectedAccess.perform(target: PathingEntity, attack: CombatAttack.Melee): Boolean {
            // The native combat dispatcher validates the minimum and deducts it after success.
            val energy = player.vars["varp.sa_energy"].coerceIn(0, 1000)
            val steps = energy / MINIMUM_ENERGY
            anim(ANIMATION)
            spotanim(EFFECT, height = 0, slot = constants.spotanim_slot_combat)
            val accurate = manager.rollMeleeAccuracy(this, target, attack.type, attack.style,
                MeleeAttackType.Stab, 1.0 + steps * 0.05)
            val damage = if (accurate) manager.rollMeleeMaxHit(this, target, attack.type, attack.style,
                1.0 + steps * 0.025).coerceAtLeast(1) else 0
            val hit = manager.queueMeleeHitIgnoringPrayer(this, target, damage, 1)
            manager.giveCombatXp(this, target, attack, hit.damage)
            val additionalCost = (energy - MINIMUM_ENERGY).coerceAtLeast(0)
            if (additionalCost > 0) manager.takeSpecialEnergy(this, additionalCost)
            manager.continueCombat(this, target)
            return true
        }
    }

    internal companion object {
        const val MINIMUM_ENERGY = 50
        const val ANIMATION = "seq.human_dragon_sword_spec"
        const val EFFECT = "spotanim.dragon_hasta_spec_spotanim"
        val WEAPONS = listOf("obj.brut_dragon_spear", "obj.brut_dragon_spear_p",
            "obj.brut_dragon_spear_p+", "obj.brut_dragon_spear_p++", "obj.brut_dragon_spear_kp")
    }
}
