package org.rsmod.api.combat.formulas.maxhit.ranged

import org.rsmod.api.combat.maxhit.player.PlayerMeleeMaxHit
import org.rsmod.api.player.bonus.WornBonuses
import org.rsmod.api.player.hands
import org.rsmod.api.player.hat
import org.rsmod.api.player.legs
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.torso
import org.rsmod.api.player.worn.EquipmentChecks
import org.rsmod.game.entity.Player

/**
 * Eclipse atlatl base damage: a wiki-confirmed unique mechanic among ranged weapons - despite
 * being ranged (accuracy still rolls off ranged level/bonus, handled separately), its **damage**
 * is computed off the player's **melee** Strength level and melee strength bonus instead of
 * ranged strength. It also uniquely reads the *ranged* void helm (not the melee one) for its void
 * bonus - "gains increased damage from Void Knight equipment whilst wearing the void ranger helm
 * rather than the void melee helm" - and melee strength prayers (Piety etc.) rather than ranged
 * ones, since the damage half of the formula is melee-shaped throughout.
 *
 * None of the atlatl's three combat styles (Accurate/Rapid/Longrange) are a melee-style
 * "aggressive" equivalent, so no style grants a bonus to the strength side of the formula here -
 * every style uses the same flat baseline `+8` every combat style implicitly gets.
 */
public object EclipseAtlatlMaxHit {
    public fun computeBaseDamage(player: Player, bonuses: WornBonuses): Int {
        val effectiveStrength =
            PlayerMeleeMaxHit.calculateEffectiveStrength(
                visibleStrengthLvl = player.stat("stat.strength"),
                styleBonus = STYLE_BONUS,
                prayerBonus = player.meleeStrengthPrayerBonus(),
                voidBonus = player.rangerHelmVoidBonus(),
                weaponBonus = 1.0,
            )
        val strengthBonus = bonuses.strengthBonus(player)
        return PlayerMeleeMaxHit.calculateBaseDamage(effectiveStrength, strengthBonus)
    }

    private fun Player.meleeStrengthPrayerBonus(): Double =
        when {
            vars["varbit.prayer_burstofstrength"] == 1 -> 1.05
            vars["varbit.prayer_superhumanstrength"] == 1 -> 1.1
            vars["varbit.prayer_ultimatestrength"] == 1 -> 1.15
            vars["varbit.prayer_chivalry"] == 1 -> 1.18
            vars["varbit.prayer_piety"] == 1 -> 1.23
            else -> 1.0
        }

    private fun Player.rangerHelmVoidBonus(): Double {
        if (!EquipmentChecks.isVoidRangerHelm(hat)) return 1.0
        if (!EquipmentChecks.isVoidGloves(hands)) return 1.0
        return when {
            EquipmentChecks.isEliteVoidTop(torso) && EquipmentChecks.isEliteVoidRobe(legs) -> 1.125
            EquipmentChecks.isRegularVoidTop(torso) && EquipmentChecks.isRegularVoidRobe(legs) -> 1.1
            else -> 1.0
        }
    }

    private const val STYLE_BONUS = 8
}
