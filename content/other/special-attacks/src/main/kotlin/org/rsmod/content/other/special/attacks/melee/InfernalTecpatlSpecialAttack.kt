package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.player.cheat.adminMaxHit
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * The Infernal tecpatl's four-hit special uses the same ordered accuracy and damage-split model
 * as dragon claws, with 25% increased accuracy and maximum hit.
 */
class InfernalTecpatlSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.infernal_tecpatl", FourfoldStrike(manager))
    }

    private class FourfoldStrike(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            fourfoldStrike(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            fourfoldStrike(target, attack)
            return true
        }

        private fun ProtectedAccess.fourfoldStrike(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.human_infernal_tecpatl_spec")

            // Wiki: "hits 4 times in quick succession with 25% increased max hit and accuracy.
            // Damage is split depending on which hits pass accuracy" - the exact same cascade
            // shape as Dragon claws' Slice and Dice, just with a 25% multiplier on both accuracy
            // and max hit instead of no bonus. Reuses the same tested cascade math.
            val maxHit =
                manager.calculateMeleeMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.25,
                )
            val hits =
                DragonClawsDamage.rollHits(
                    maxHit = maxHit,
                    rollAccuracy = {
                        manager.rollMeleeAccuracy(
                            source = this,
                            target = target,
                            attackType = attack.type,
                            attackStyle = attack.style,
                            blockType = attack.type,
                            multiplier = 1.25,
                        )
                    },
                    rollRange = { range -> rollRange(range.first, range.last) },
                    rollSympathyTriggers = { random.of(3) < 2 },
                    rollSympathyPattern = { random.of(4) },
                )

            var totalDamage = 0
            for (damage in hits) {
                totalDamage += damage
                manager.queueMeleeHit(this, target, damage)
            }
            manager.giveCombatXp(this, target, attack, totalDamage)
            manager.continueCombat(this, target)
        }

        private fun ProtectedAccess.rollRange(min: Int, max: Int): Int =
            DragonClawsDamage.resolveRange(
                min = min,
                max = max,
                isMaxHit = player.adminMaxHit,
                rollInclusive = { lo, hi -> random.of(lo, hi) },
                rollUpTo = { exclusiveBound -> random.of(maxExclusive = exclusiveBound) },
            )
    }
}
