package org.alter.combat

import org.alter.api.CombatAttributes
import org.alter.api.WeaponType
import org.alter.api.ext.getAttackStyle
import org.alter.api.ext.hasWeaponType
import org.alter.game.model.combat.AttackStyle
import org.alter.game.model.combat.CombatStyle
import org.alter.game.model.entity.Player

/**
 * Shared utility that resolves a [Player]'s [AttackStyle] and [CombatStyle]
 * from their current weapon type (varbit) and combat mode varp.
 *
 * The attack-style mapping is extracted from MeleeCombatFormulaPlugin and the
 * combat-style mapping is ported from CombatConfigs.getCombatStyle (game-plugins).
 */
object AttackStyleResolver {

    /**
     * Maps the player's weapon type + attack-style varp to an [AttackStyle].
     */
    fun getAttackStyle(player: Player): AttackStyle {
        val style = player.getAttackStyle()

        return when {
            player.hasWeaponType(WeaponType.NONE) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.AGGRESSIVE
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.THROWN, WeaponType.CHINCHOMPA) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.RAPID
                3 -> AttackStyle.LONG_RANGE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.TRIDENT) -> when (style) {
                0, 1 -> AttackStyle.ACCURATE
                3 -> AttackStyle.LONG_RANGE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.SPEAR) -> when (style) {
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.CONTROLLED
            }

            player.hasWeaponType(WeaponType.HALBERD, WeaponType.STAFF_HALBERD) -> when (style) {
                0 -> AttackStyle.CONTROLLED
                1 -> AttackStyle.AGGRESSIVE
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.SCYTHE) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1, 2 -> AttackStyle.AGGRESSIVE
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.WHIP) -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.CONTROLLED
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }

            player.hasWeaponType(WeaponType.BLUDGEON) -> AttackStyle.AGGRESSIVE

            player.hasWeaponType(WeaponType.BULWARK) -> AttackStyle.ACCURATE

            // Standard melee: AXE, HAMMER, TWO_HANDED, PICKAXE, DAGGER, MAGIC_STAFF,
            //                  LONG_SWORD, CLAWS, MACE, SALAMANDER, GUN, STAFF
            else -> when (style) {
                0 -> AttackStyle.ACCURATE
                1 -> AttackStyle.AGGRESSIVE
                2 -> AttackStyle.CONTROLLED
                3 -> AttackStyle.DEFENSIVE
                else -> AttackStyle.NONE
            }
        }
    }

    /**
     * Maps the player's weapon type + attack-style varp to a [CombatStyle].
     * Magic detection via [CombatAttributes.CASTING_SPELL] takes precedence.
     */
    fun getCombatStyle(player: Player): CombatStyle {
        val style = player.getAttackStyle()

        return when {
            player.attr[CombatAttributes.CASTING_SPELL] != null -> CombatStyle.MAGIC

            player.hasWeaponType(WeaponType.NONE) -> CombatStyle.CRUSH

            player.hasWeaponType(WeaponType.BOW, WeaponType.CROSSBOW, WeaponType.THROWN, WeaponType.CHINCHOMPA) -> CombatStyle.RANGED

            player.hasWeaponType(WeaponType.TRIDENT) -> CombatStyle.MAGIC

            player.hasWeaponType(WeaponType.AXE) -> when (style) {
                2 -> CombatStyle.CRUSH
                else -> CombatStyle.SLASH
            }

            player.hasWeaponType(WeaponType.HAMMER) -> CombatStyle.CRUSH

            player.hasWeaponType(WeaponType.CLAWS) -> when (style) {
                2 -> CombatStyle.STAB
                else -> CombatStyle.SLASH
            }

            player.hasWeaponType(WeaponType.SALAMANDER) -> when (style) {
                0 -> CombatStyle.SLASH
                1 -> CombatStyle.RANGED
                else -> CombatStyle.MAGIC
            }

            player.hasWeaponType(WeaponType.LONG_SWORD) -> when (style) {
                2 -> CombatStyle.STAB
                else -> CombatStyle.SLASH
            }

            player.hasWeaponType(WeaponType.TWO_HANDED) -> when (style) {
                2 -> CombatStyle.CRUSH
                else -> CombatStyle.SLASH
            }

            player.hasWeaponType(WeaponType.PICKAXE) -> when (style) {
                2 -> CombatStyle.CRUSH
                else -> CombatStyle.STAB
            }

            player.hasWeaponType(WeaponType.HALBERD) -> when (style) {
                1 -> CombatStyle.SLASH
                else -> CombatStyle.STAB
            }

            player.hasWeaponType(WeaponType.STAFF) -> CombatStyle.CRUSH

            player.hasWeaponType(WeaponType.SCYTHE) -> when (style) {
                2 -> CombatStyle.CRUSH
                else -> CombatStyle.SLASH
            }

            player.hasWeaponType(WeaponType.SPEAR) -> when (style) {
                1 -> CombatStyle.SLASH
                2 -> CombatStyle.CRUSH
                else -> CombatStyle.STAB
            }

            player.hasWeaponType(WeaponType.MACE) -> when (style) {
                2 -> CombatStyle.STAB
                else -> CombatStyle.CRUSH
            }

            player.hasWeaponType(WeaponType.DAGGER) -> when (style) {
                2 -> CombatStyle.SLASH
                else -> CombatStyle.STAB
            }

            player.hasWeaponType(WeaponType.MAGIC_STAFF) -> CombatStyle.CRUSH

            player.hasWeaponType(WeaponType.WHIP) -> CombatStyle.SLASH

            player.hasWeaponType(WeaponType.STAFF_HALBERD) -> when (style) {
                0 -> CombatStyle.STAB
                1 -> CombatStyle.SLASH
                else -> CombatStyle.CRUSH
            }

            player.hasWeaponType(WeaponType.BLUDGEON) -> CombatStyle.CRUSH

            player.hasWeaponType(WeaponType.BULWARK) -> when (style) {
                0 -> CombatStyle.CRUSH
                else -> CombatStyle.NONE
            }

            else -> CombatStyle.NONE
        }
    }
}
