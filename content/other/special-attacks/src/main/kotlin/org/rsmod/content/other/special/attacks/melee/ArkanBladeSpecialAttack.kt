package org.rsmod.content.other.special.attacks.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.config.constants
import org.rsmod.api.mechanics.toxins.BurnEffectService
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.combat.MeleeSpecialAttack
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Flames of Ralos makes one 150%-accurate, 150%-maximum hit against Slash defence. A successful
 * accuracy roll applies the shared Burn status, even if a later target-specific damage modifier
 * reduces the direct hit to zero.
 */
class ArkanBladeSpecialAttack
@Inject
constructor(
    private val burns: BurnEffectService,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerMelee("obj.arkan_blade", FlamesOfRalos(manager, burns))
    }

    private class FlamesOfRalos(
        private val manager: SpecialAttackManager,
        private val burns: BurnEffectService,
    ) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            flamesOfRalos(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            flamesOfRalos(target, attack)
            return true
        }

        private fun ProtectedAccess.flamesOfRalos(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ) {
            anim("seq.vmq4_arkan_blade_special")
            spotanim(
                spot = "spotanim.vmq4_arkan_blade_special_spotanim",
                slot = constants.spotanim_slot_combat,
                // 96 (blindly copied from Dragon claws) still floated too high even at 48 per
                // live feedback - dropped to ground level. Visual-tuning guess, not verified
                // against a real screenshot; needs your eyes to confirm.
                height = 0,
            )

            val successful =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = MeleeAttackType.Slash,
                    multiplier = 1.5,
                )
            val damage =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.5,
                    )
                } else {
                    0
                }

            if (successful) {
                burns.apply(source = player, target = target)
            }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
        }
    }
}
