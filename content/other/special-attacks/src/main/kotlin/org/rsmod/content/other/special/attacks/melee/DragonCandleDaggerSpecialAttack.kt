package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Celebrate is deliberately an attack that always deals zero damage.
 *
 * The game has a free-to-play world restriction for this cosmetic special. OpenRune's realm
 * configuration has no membership-world state, so the combat behaviour is kept exact while that
 * unavailable world gate is left to a future realm-membership implementation.
 */
class DragonCandleDaggerSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.osb10_dragon_candle", Celebrate(manager))
    }

    private class Celebrate(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = celebrate(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = celebrate(target, attack)

        private fun ProtectedAccess.celebrate(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            // This special never rolls accuracy or damage: Celebrate always produces a zero hit.
            anim("seq.bday23_candle_special")
            manager.giveCombatXp(this, target, attack, damage = 0)
            manager.queueMeleeHit(this, target, damage = 0)
            manager.continueCombat(this, target)
            return true
        }
    }
}
