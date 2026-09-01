package org.rsmod.content.other.special.weapons.melee

import jakarta.inject.Inject
import org.rsmod.api.combat.commons.CombatAttack
import org.rsmod.api.mechanics.toxins.SoulreaperStackDecay
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.weapons.MeleeWeapon
import org.rsmod.api.weapons.WeaponAttackManager
import org.rsmod.api.weapons.WeaponMap
import org.rsmod.api.weapons.WeaponRepository
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.PathingEntity
import org.rsmod.game.entity.Player

/**
 * Ordinary Crush attacks; the axe's only unusual behavior is Soul Stacks. Wiki: every attack with
 * the axe generates a stack (even on a miss) until five are held, applied after this swing's own
 * damage is calculated - so the swing that generates a stack doesn't benefit from the Strength
 * bonus it just earned. The Strength bonus itself is read directly off the stack count elsewhere
 * (`MeleeMaxHitOperations.calculateEffectiveStrength`); this file only owns generating/capping the
 * stack and resetting the decay timer (`SoulreaperStackDecay`, `SoulreaperStackDecayScript`).
 */
class SoulreaperAxeWeapons @Inject constructor() : WeaponMap {
    override fun WeaponRepository.register(manager: WeaponAttackManager) {
        val axe = SoulreaperAxe(manager)
        register("obj.soulreaper", axe)
        register("obj.soulreaper_axe_orn", axe)
    }

    private class SoulreaperAxe(private val manager: WeaponAttackManager) : MeleeWeapon {
        override suspend fun ProtectedAccess.attack(
            target: Npc,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        override suspend fun ProtectedAccess.attack(
            target: Player,
            attack: CombatAttack.Melee,
        ): Boolean = swing(target, attack)

        private fun ProtectedAccess.swing(
            target: PathingEntity,
            attack: CombatAttack.Melee,
        ): Boolean {
            manager.playWeaponFx(this, attack)
            val damage =
                manager.rollMeleeDamage(
                    source = this,
                    target = target,
                    attack = attack,
                    accuracyMultiplier = 1.0,
                    maxHitMultiplier = 1.0,
                )
            manager.giveCombatXp(this, target, attack, damage)
            manager.queueMeleeHit(this, target, damage)
            gainSoulStack()
            manager.continueCombat(this, target)
            return true
        }

        private fun ProtectedAccess.gainSoulStack() {
            val stacks = vars[SOUL_STACKS].coerceIn(0, MAX_SOUL_STACKS)
            val next = SoulreaperStackGain.nextStackCount(stacks, MAX_SOUL_STACKS)
            if (next != stacks) {
                VarPlayerIntMapSetter.set(player, SOUL_STACKS, next)
            }
            // Attacking with the axe at all is what keeps stacks alive, independent of whether
            // this particular swing was capped out and generated nothing.
            SoulreaperStackDecay.reset(player)
        }
    }

    private companion object {
        const val SOUL_STACKS = "varp.soulreaper_stacks"
        const val MAX_SOUL_STACKS = 5
    }
}

/** Pure Soul Stack cap math, kept separate from [ProtectedAccess] so it can be unit tested. */
internal object SoulreaperStackGain {
    /** Wiki: a stack is generated on every attack, up to a maximum of five. */
    fun nextStackCount(current: Int, max: Int): Int = (current + 1).coerceAtMost(max)
}
