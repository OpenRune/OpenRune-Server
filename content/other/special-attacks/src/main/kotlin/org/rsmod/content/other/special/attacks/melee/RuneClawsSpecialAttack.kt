package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
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
 * Impale is a single hit with 10% extra accuracy and maximum damage. It takes one cycle longer
 * than the Rune claws' normal four-cycle attack.
 */
class RuneClawsSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.rune_claws", Impale(manager))
    }

    private class Impale(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            impale(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            impale(target, attack)
            return true
        }

        private fun ProtectedAccess.impale(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.impale")
            spotanim(
                spot = "spotanim.sp_attack_impale_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 96,
            )

            // Rune claws normally attack every four cycles; Impale is a five-cycle attack.
            manager.setNextAttackDelay(this, 5)

            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.1,
                    maxHitMultiplier = 1.1,
                    blockType = attack.type,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }
}
