package org.rsmod.content.other.special.attacks.ranged

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.BurnEffectService
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.api.random.GameRandom
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.RangedSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Eclipse is a point-blank, Magic-based ranged hit. It consumes remaining Burn damage even if the
 * accuracy roll fails, turning it into a capped minimum and maximum damage increase for this hit.
 */
class EclipseAtlatlSpecialAttack
@Inject
constructor(
    private val burns: BurnEffectService,
    private val random: GameRandom,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val eclipse = Eclipse(manager, burns, random)
        registerRanged("obj.eclipse_atlatl", eclipse)
        registerRanged("obj.br_eclipse_atlatl", eclipse)
    }

    private class Eclipse(
        private val manager: SpecialAttackManager,
        private val burns: BurnEffectService,
        private val random: GameRandom,
    ) : RangedSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Ranged,
        ): Boolean = eclipse(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Ranged,
        ): Boolean = eclipse(target, attack)

        private fun ProtectedAccess.eclipse(
            target: PathingEntity,
            attack: CombatAttack.Ranged,
        ): Boolean {
            if (!player.isWearingEclipseMoonSet()) {
                mes("You need to be wearing the full Eclipse Moon armour set to use this special attack.")
                manager.stopCombat(this)
                return false
            }
            if (distanceTo(target) > MELEE_DISTANCE) {
                mes("You need to be within melee distance of your target to use this special attack.")
                manager.stopCombat(this)
                return false
            }

            anim("seq.human_special_atlatl_01")
            spotanim(
                spot = "spotanim.special_atlatl_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )

            val normalMax =
                manager.calculateRangedMaxHit(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                    boltSpecDamage = 0,
                )
            val hitRange =
                EclipseAtlatlDamage.hitRange(
                    normalMax = normalMax,
                    remainingBurn = burns.consumeRemainingDamage(target),
                )
            val damage =
                if (
                    manager.rollMagicalRangedAccuracy(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = ACCURACY_MULTIPLIER,
                    )
                ) {
                    random.of(hitRange)
                } else {
                    0
                }

            // The hit is Magic-type for protection prayers, but the atlatl still trains Ranged.
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(
                source = this,
                target = target,
                damage = damage,
                clientDelay = 0,
            )
            target.spotanim(
                spot = "spotanim.special_atlatl_impact_spotanim",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )
            manager.continueCombat(this, target)
            return true
        }
    }

    private companion object {
        const val MELEE_DISTANCE: Int = 1
        const val ACCURACY_MULTIPLIER: Double = 1.5
    }
}

/** Pure Burn-to-hit-range conversion used by Eclipse. */
internal object EclipseAtlatlDamage {
    fun hitRange(normalMax: Int, remainingBurn: Int): IntRange {
        require(normalMax >= 0) {
            "normalMax must not be negative. (normalMax=$normalMax)"
        }
        val burnBonus = remainingBurn.coerceIn(0, MAX_BURN_DAMAGE)
        return (burnBonus / 2)..(normalMax + burnBonus)
    }

    private const val MAX_BURN_DAMAGE: Int = 50
}

internal fun Player.isWearingEclipseMoonSet(): Boolean =
    EquipmentChecks.isEclipseMoonSet(hat, torso, legs, righthand)
