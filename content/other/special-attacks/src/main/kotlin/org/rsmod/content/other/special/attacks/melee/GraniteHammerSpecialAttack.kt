package org.rsmod.content.other.special.attacks.melee

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

/**
 * Hammer Blow uses Crush defence with 50% extra accuracy, then adds five damage to the resulting
 * roll. The addition is deliberate: a failed accuracy roll becomes the special's five-damage
 * minimum hit.
 */
class GraniteHammerSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.granite_hammer", HammerBlow(manager))
    }

    private class HammerBlow(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            hammerBlow(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            hammerBlow(target, attack)
            return true
        }

        private fun ProtectedAccess.hammerBlow(target: PathingEntity, attack: CombatAttack.Melee) {
            // Revision-240 uses the dragon warhammer special sequence for this strike.
            anim("seq.dragon_warhammer_sa_player")
            spotanim(
                spot = "spotanim.granite_hammer_sa_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.5,
                    maxHitMultiplier = 1.0,
                    blockType = MeleeAttackType.Crush,
                ) + 5
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }
}
