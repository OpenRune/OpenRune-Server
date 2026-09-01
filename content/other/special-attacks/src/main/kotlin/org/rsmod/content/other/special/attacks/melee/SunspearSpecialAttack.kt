package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.stat.hitpoints
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Seeking Lunge deals exactly 70% of the wielder's normal maximum melee hit on a successful hit.
 *
 * When that fixed damage would defeat the target, its attack roll is fixed at 70% instead of being
 * randomized. The target still rolls its normal defence.
 */
class SunspearSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.sunspear", SeekingLunge(manager))
    }

    private class SeekingLunge(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            seekingLunge(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            seekingLunge(target, attack)
            return true
        }

        private fun ProtectedAccess.seekingLunge(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.human_weapons_sunspear_spec")
            spotanim("spotanim.vfx_sunspear_special")
            val maxHit =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val fixedDamage = (maxHit * DAMAGE_MULTIPLIER).toInt()
            // Wiki: "Seeking Lunge will roll with exactly 70% of its maximum accuracy" when the
            // fixed damage would defeat the target - i.e. the same accuracy roll as normal, just
            // with a 70% multiplier applied instead of 100%. This engine's attack roll is already
            // a deterministic value (no RNG) scaled by `multiplier` before the single random
            // hit/miss sample, so this is exactly the standard roll with a different multiplier -
            // no separate "fixed roll" mechanism needed.
            val accuracyMultiplier =
                if (target.hitpoints() <= fixedDamage) FIXED_ACCURACY_MULTIPLIER else 1.0
            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = accuracyMultiplier,
                )
            val damage = if (accurate) fixedDamage else 0
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }

        private companion object {
            const val DAMAGE_MULTIPLIER: Double = 0.70
            const val FIXED_ACCURACY_MULTIPLIER: Double = 0.70
        }
    }
}

private fun PathingEntity.hitpoints(): Int =
    when (this) {
        is Npc -> hitpoints
        is Player -> hitpoints
    }
