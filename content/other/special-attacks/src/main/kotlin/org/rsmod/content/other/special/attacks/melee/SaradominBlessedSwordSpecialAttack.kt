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
 * Saradomin's Blessed Lightning is Magic-based melee damage: its accuracy is the wielder's Slash
 * attack bonus against Magic defence, while its maximum hit is the normal melee maximum ? 1.25.
 */
class SaradominBlessedSwordSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val lightning = BlessedLightning(manager)
        registerMelee("obj.blessed_saradomin_sword", lightning)
        registerMelee("obj.blessed_saradomin_sword_degraded", lightning)
    }

    private class BlessedLightning(private val manager: SpecialAttackManager) : MeleeSpecialAttack {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean {
            lightning(target, attack)
            return true
        }

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean {
            lightning(target, attack)
            return true
        }

        private fun ProtectedAccess.lightning(target: PathingEntity, attack: CombatAttack.Melee) {
            anim("seq.blessed_saradomin_sword_special_player")
            target.spotanim(
                spot = "spotanim.saradomin_lightning",
                height = 96,
                slot = constants.spotanim_slot_combat,
            )

            val successful =
                manager.rollMagicalMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = MeleeAttackType.Slash,
                    attackStyle = attack.style,
                    multiplier = 1.0,
                )
            val damage =
                if (successful) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.25,
                    )
                } else {
                    0
                }

            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMagicHit(this, target, damage, clientDelay = 0)
            manager.continueCombat(this, target)
        }
    }
}
