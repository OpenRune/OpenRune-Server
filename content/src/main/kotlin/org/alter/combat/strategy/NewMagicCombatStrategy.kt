package org.alter.combat.strategy

import org.alter.api.CombatAttributes
import org.alter.combat.spell.CombatSpell
import org.alter.game.combat.CombatStrategy
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player

/**
 * New-engine magic combat strategy implementing [CombatStrategy].
 *
 * Ported from [org.alter.plugins.content.combat.strategy.MagicCombatStrategy].
 */
class NewMagicCombatStrategy : CombatStrategy {

    companion object {
        /** Standard spell attack speed in ticks (matches legacy MagicCombatStrategy). */
        private const val STANDARD_SPELL_SPEED = 5
    }

    override fun getAttackRange(attacker: Pawn): Int = 10

    override fun getAttackSpeed(attacker: Pawn): Int {
        if (attacker is Npc) {
            return attacker.combatDef.attackSpeed
        }
        // Players always cast at the standard spell tick rate.
        // TODO: Trident-class weapons have their own attack speed via weapon def — check
        //       weapon type once WeaponType lookup is available here.
        return STANDARD_SPELL_SPEED
    }

    override fun getAttackAnimation(attacker: Pawn): String {
        if (attacker is Player) {
            val spell = attacker.attr[CombatAttributes.CASTING_SPELL] as? CombatSpell
            if (spell != null) return spell.castAnimation
        }
        if (attacker is Npc) return attacker.combatDef.attackAnimation
        return "sequences.human_cast_magic"
    }

    /**
     * Hit delay for magic: `2 + floor((1 + distance) / 3)`.
     *
     * Distance is measured between the attacker and target centre tiles,
     * matching [org.alter.plugins.content.combat.strategy.MagicCombatStrategy.getHitDelay].
     */
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int {
        val distance = attacker.getCentreTile().getDistance(target.getCentreTile())
        return 2 + Math.floor((1.0 + distance) / 3.0).toInt()
    }
}
