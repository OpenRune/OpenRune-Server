package org.rsmod.content.other.special.attacks.melee

import dev.openrune.types.ItemServerType
import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.combat.commons.CombatStance
import org.rsmod.api.combat.commons.magic.Spellbook
import org.rsmod.api.combat.commons.styles.MeleeAttackStyle
import org.rsmod.api.combat.commons.types.MeleeAttackType
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.combat.weapon.types.AttackTypes
import org.rsmod.api.config.constants
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
 * Ferocity is an instant ordinary melee hit that guarantees Flames of Cerberus when its melee
 * accuracy roll succeeds. Its instant attack still imposes the Fang's normal three-cycle delay.
 */
class FangOfTheHoundSpecialAttack
@Inject
constructor(
    private val attackTypes: AttackTypes,
    private val attackStyles: AttackStyles,
) : SpecialAttackMap {
    override fun SpecialAttackRepository.register(manager: SpecialAttackManager) {
        registerInstant(
            "obj.fang_of_the_hound",
            Ferocity(
                manager = manager,
                attackTypes = attackTypes,
                attackStyles = attackStyles,
            ),
        )
    }

    private class Ferocity(
        private val manager: SpecialAttackManager,
        private val attackTypes: AttackTypes,
        private val attackStyles: AttackStyles,
    ) : InstantSpecialAttack {
        override suspend fun ProtectedAccess.activate(): Boolean {
            val target = currentCombatTarget() ?: return false
            if (!isWithinDistance(target, MELEE_RANGE)) {
                return false
            }

            val attack = currentMeleeAttack() ?: return false
            anim("seq.human_karambit_spec")

            val accurate =
                manager.rollMeleeAccuracy(
                    source = this,
                    target = target,
                    attackType = attack.type,
                    attackStyle = attack.style,
                    blockType = attack.type,
                    multiplier = 1.0,
                )
            val damage =
                if (accurate) {
                    manager.rollMeleeMaxHit(
                        source = this,
                        target = target,
                        attackType = attack.type,
                        attackStyle = attack.style,
                        multiplier = 1.0,
                    )
                } else {
                    0
                }

            manager.queueMeleeHit(this, target, damage)
            if (accurate) {
                flamesOfCerberus(target)
            }
            manager.giveCombatXp(this, target, attack, damage)
            manager.setNextAttackDelay(this, FANG_ATTACK_RATE)
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.currentCombatTarget(): PathingEntity? =
            when (val interaction = player.interaction) {
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

        private fun ProtectedAccess.currentMeleeAttack(): CombatAttack.Melee? {
            val weapon = player.righthand ?: return null
            val stance = CombatStance[player.vars[COMBAT_STANCE_VARP]] ?: CombatStance.Stance1
            return CombatAttack.Melee(
                weapon = weapon,
                type = attackTypes.get(player) as? MeleeAttackType,
                style = attackStyles.get(player) as? MeleeAttackStyle,
                stance = stance,
            )
        }

        private fun ProtectedAccess.flamesOfCerberus(target: PathingEntity) {
            val damage =
                manager.rollSpellMaxHit(
                    source = this,
                    target = target,
                    spell = FLAMES_OF_CERBERUS,
                    spellbook = Spellbook.Standard,
                    baseMaxHit = FLAMES_BASE_MAX_HIT,
                    attackRate = FANG_ATTACK_RATE,
                )

            // Same fix as the passive's identical effect in FangOfTheHoundWeapons.kt - this is a
            // separate copy of the same spotanim call, not shared code, so it needed its own fix.
            target.spotanim(
                spot = "spotanim.vfx_flames_of_cerberus",
                slot = constants.spotanim_slot_combat,
                height = 0,
            )
            manager.queueMagicHit(
                source = this,
                target = target,
                damage = damage,
                clientDelay = 0,
                spell = FLAMES_OF_CERBERUS,
            )
        }
    }

    private companion object {
        const val COMBAT_STANCE_VARP: String = "varp.com_mode"
        const val MELEE_RANGE: Int = 1
        const val FANG_ATTACK_RATE: Int = 3
        const val FLAMES_BASE_MAX_HIT: Int = 10

        // Cache item: obj.fang_of_the_hound_fire (id 33377).
        val FLAMES_OF_CERBERUS: ItemServerType = ItemServerType(33377)
    }
}
