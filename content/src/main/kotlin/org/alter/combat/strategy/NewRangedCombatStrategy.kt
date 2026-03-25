package org.alter.combat.strategy

import org.alter.api.EquipmentType
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.getEquipment
import org.alter.api.ext.hasWeaponType
import org.alter.game.combat.CombatStrategy
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.entity.Npc
import org.alter.game.model.entity.Pawn
import org.alter.game.model.entity.Player
import org.alter.rscm.RSCM.getRSCM

/**
 * New-engine ranged combat strategy implementing [CombatStrategy].
 *
 * Ported from [org.alter.plugins.content.combat.strategy.RangedCombatStrategy]
 * and [org.alter.plugins.content.combat.CombatConfigs].
 *
 * Attack style resolution (RAPID / LONG_RANGE) is inlined because CombatConfigs
 * lives in game-plugins which the content module cannot import.
 */
class NewRangedCombatStrategy : CombatStrategy {

    companion object {
        private const val DEFAULT_ATTACK_RANGE = 7
        private const val MAX_ATTACK_RANGE = 10
        private const val DEFAULT_ATTACK_SPEED = 4
        private const val MIN_ATTACK_SPEED = 1
    }

    override fun getAttackRange(attacker: Pawn): Int {
        if (attacker is Player) {
            val weapon = attacker.getEquipment(EquipmentType.WEAPON)
            val attackStyle = resolveAttackStyle(attacker)

            var range = when (weapon?.id) {
                getRSCM("items.acb") -> 8
                getRSCM("items.wild_cave_bow_charged"),
                getRSCM("items.wild_cave_bow_uncharged") -> 10
                getRSCM("items.chinchompa_captured"),
                getRSCM("items.chinchompa_big_captured"),
                getRSCM("items.chinchompa_black") -> 9
                // TODO: add Bows.LONG_BOWS range-9 check when bow item sets are moved to a shared module
                // TODO: add Knives.KNIVES range-6 check when knife item sets are moved to a shared module
                // TODO: add Darts.DARTS range-3 check when dart item sets are moved to a shared module
                // TODO: add Bows.CRYSTAL_BOWS range-10 check when crystal bow sets are moved to a shared module
                else -> DEFAULT_ATTACK_RANGE
            }

            if (attackStyle == AttackStyle.LONG_RANGE) {
                range += 2
            }

            return minOf(MAX_ATTACK_RANGE, range)
        }
        return DEFAULT_ATTACK_RANGE
    }

    override fun getAttackSpeed(attacker: Pawn): Int {
        if (attacker is Npc) {
            return attacker.combatDef.attackSpeed
        }
        if (attacker is Player) {
            val baseSpeed = attacker.getEquipment(EquipmentType.WEAPON)
                ?.getDef()?.weapon?.attackSpeed
                ?.let { maxOf(MIN_ATTACK_SPEED, it) }
                ?: DEFAULT_ATTACK_SPEED

            // Rapid style reduces attack speed by 1 tick
            val attackStyle = resolveAttackStyle(attacker)
            return if (attackStyle == AttackStyle.RAPID) maxOf(MIN_ATTACK_SPEED, baseSpeed - 1) else baseSpeed
        }
        return DEFAULT_ATTACK_SPEED
    }

    override fun getAttackAnimation(attacker: Pawn): String {
        if (attacker is Npc) {
            return attacker.combatDef.attackAnimation
        }
        if (attacker is Player) {
            return when {
                attacker.hasWeaponType(WeaponType.BOW) -> "sequences.human_bow"
                attacker.hasWeaponType(WeaponType.CROSSBOW) -> "sequences.xbows_human_fire_and_reload"
                attacker.hasWeaponType(WeaponType.CHINCHOMPA) -> "sequences.human_chinchompa_attack_pvn"
                attacker.hasWeaponType(WeaponType.THROWN) -> "sequences.human_stake2"
                // TODO: add specific thrown weapon animations (e.g. tzhaar ring) when item sets are in a shared module
                else -> "sequences.human_bow"
            }
        }
        return "sequences.human_bow"
    }

    /**
     * Hit delay for ranged: `2 + floor((3 + distance) / 6)`.
     *
     * Distance is measured from the attacker's centre tile to the target's centre tile,
     * matching [org.alter.plugins.content.combat.strategy.RangedCombatStrategy.getHitDelay].
     */
    override fun getHitDelay(attacker: Pawn, target: Pawn): Int {
        val distance = attacker.getCentreTile().getDistance(target.getCentreTile())
        return 2 + Math.floor((3.0 + distance) / 6.0).toInt()
    }

    // ---------------------------------------------------------------
    // Inlined attack style resolution (from CombatConfigs.getAttackStyle)
    // ---------------------------------------------------------------

    private fun resolveAttackStyle(player: Player): AttackStyle {
        val style = player.getAttackStyle()
        return when {
            player.hasWeaponType(WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.THROWN, WeaponType.CHINCHOMPA) ->
                when (style) {
                    0 -> AttackStyle.ACCURATE
                    1 -> AttackStyle.RAPID
                    3 -> AttackStyle.LONG_RANGE
                    else -> AttackStyle.NONE
                }
            else -> AttackStyle.NONE
        }
    }
}
