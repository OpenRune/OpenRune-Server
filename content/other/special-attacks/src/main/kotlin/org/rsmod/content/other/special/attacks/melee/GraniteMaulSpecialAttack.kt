package org.rsmod.content.other.special.attacks.melee

import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.npc.isValidTarget as isValidNpcTarget
import org.rsmod.api.player.isValidTarget as isValidPlayerTarget
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.righthand
import org.rsmod.api.specials.SpecialAttackManager
import org.rsmod.api.specials.SpecialAttackMap
import org.rsmod.api.specials.SpecialAttackRepository
import org.rsmod.api.specials.instant.InstantSpecialAttack
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.interact.InteractionNpcOp
import org.rsmod.game.interact.InteractionOp
import org.rsmod.game.interact.InteractionPlayerOp

/**
 * Quick Smash is an instant, ordinary Crush attack. It deliberately does not alter the player's
 * normal attack timer: a second valid click can therefore produce the genuine second smash when
 * the cache-provided energy cost permits it (the 50% variants), while the 60% variants cannot.
 */
class GraniteMaulSpecialAttack : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        val quickSmash = QuickSmash(manager)

        // Rev-240: the unupgraded and ornamental mauls retain the 60% cost, while the Battle
        // Royale and + variants use 50%. The repository reads those costs from cache data.
        registerInstant("obj.granite_maul", quickSmash)
        registerInstant("obj.granite_maul_pretty", quickSmash)
        registerInstant("obj.br_granite_maul", quickSmash)
        registerInstant("obj.granite_maul_plus", quickSmash)
        registerInstant("obj.granite_maul_pretty_plus", quickSmash)
    }

    private class QuickSmash(private val manager: SpecialAttackManager) : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            val target = currentCombatTarget() ?: return false
            if (!isWithinDistance(target, MELEE_RANGE)) {
                return false
            }

            val weapon = player.righthand ?: return false
            val attack =
                CombatAttack.Melee(
                    weapon = weapon,
                    type = MeleeAttackType.Crush,
                    style = graniteMaulStyle(),
                    stance = CombatStance[player.vars[COMBAT_STANCE_VARP]] ?: CombatStance.Stance1,
                )

            anim("seq.slayer_granite_maul_special_attack")
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = 1.0,
                    attackType = MeleeAttackType.Crush,
                    blockType = MeleeAttackType.Crush,
                )
            manager.giveCombatXp(this, target, attack, damage)
            // Normal melee impact timing is retained. Consecutive instant button presses queue
            // their hits for the same following cycle, which is the maul's rapid double smash.
            manager.queueMeleeHit(this, target, damage)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.currentCombatTarget(): PathingEntity? {
            return when (val interaction = player.interaction) {
                is InteractionNpcOp -> {
                    if (interaction.op != InteractionOp.Op2) {
                        null
                    } else {
                        findUid(interaction.uid)?.takeIf { it.isValidNpcTarget() }
                    }
                }

                is InteractionPlayerOp -> {
                    if (interaction.op != InteractionOp.Op1 && interaction.op != InteractionOp.Op2) {
                        null
                    } else {
                        findUid(interaction.uid)?.takeIf { it.isValidPlayerTarget() }
                    }
                }

                else -> null
            }
        }

        private fun ProtectedAccess.graniteMaulStyle(): MeleeAttackStyle? =
            GraniteMaulStyle.resolve(CombatStance[player.vars[COMBAT_STANCE_VARP]])
    }

    private companion object {
        const val COMBAT_STANCE_VARP = "varp.com_mode"
        const val MELEE_RANGE = 1
    }
}

/** Pure stance-to-style mapping, kept separate from [ProtectedAccess] so it can be unit tested. */
internal object GraniteMaulStyle {
    fun resolve(stance: CombatStance?): MeleeAttackStyle? =
        when (stance) {
            CombatStance.Stance1 -> MeleeAttackStyle.Accurate
            CombatStance.Stance2 -> MeleeAttackStyle.Aggressive
            CombatStance.Stance3,
            CombatStance.Stance4,
            null -> MeleeAttackStyle.Defensive
        }
}
