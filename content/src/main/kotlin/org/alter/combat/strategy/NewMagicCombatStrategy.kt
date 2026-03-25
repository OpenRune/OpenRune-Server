package org.alter.combat.strategy

import org.alter.game.combat.CombatStrategy
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn

/**
 * New-engine magic combat strategy implementing [CombatStrategy].
 *
 * Ported from [org.alter.plugins.content.combat.strategy.MagicCombatStrategy].
 *
 * Animation resolution for players is stubbed because the casting spell is stored
 * in an AttributeKey defined in game-plugins (Combat.CASTING_SPELL), which is not
 * accessible from the content module. The cast animation is spell-specific and will
 * require moving that attribute key to a shared module (game-server or game-api)
 * before full resolution is possible.
 *
 * TODO: Replace getAttackAnimation stub once Combat.CASTING_SPELL attribute key is
 *       moved to a shared module (game-server or game-api).
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
        if (attacker is Npc) {
            return attacker.combatDef.attackAnimation
        }
        // TODO: Read the active spell's castAnimation from attacker.attr[Combat.CASTING_SPELL]
        //       once Combat.CASTING_SPELL is moved to a shared module. For now, return the
        //       generic magic cast animation as a safe fallback.
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
