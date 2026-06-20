package org.rsmod.content.areas.wilderness

import jakarta.inject.Inject
import kotlin.math.abs
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.config.constants
import org.rsmod.api.death.LAST_PVP_HIT_TICK_ATTR
import org.rsmod.api.death.PvPAttackValidateHook
import org.rsmod.api.death.PvPAttackValidateResult
import org.rsmod.api.death.PvPPlayerHitHook
import org.rsmod.api.death.PvPSkullHook
import org.rsmod.api.player.isInPvpCombat
import org.rsmod.content.areas.wilderness.WildernessAreaScript.Companion.canPvp
import org.rsmod.game.entity.Player
import org.rsmod.map.CoordGrid

public class WildernessPvPHook @Inject constructor(private val areaChecker: AreaChecker) :
    PvPAttackValidateHook, PvPSkullHook, PvPPlayerHitHook {

    override fun validate(attacker: Player, target: Player): PvPAttackValidateResult {
        if (!attacker.canPvp()) {
            return PvPAttackValidateResult.Pass
        }

        if (areaChecker.inArea("area.ferox_enclave", target.coords)
            && target.vars["varbit.teleblock_cycles"] <= 0
            && attacker.vars["varbit.teleblock_cycles"] <= 0
        ) {
            return PvPAttackValidateResult.Deny(
                "You cannot fight another player whilst next to the Enclave, please move further out.",
            )
        }

        if (!attacker.isSkulled()
            && attacker.isSkullPreventionEnabled()
            && wouldSkull(attacker, target)
        ) {
            return PvPAttackValidateResult.Deny(
                "You cannot attack this target as it would result in you getting skulled.",
            )
        }

        if (attacker.canPvp() && !target.canPvp()) {
            return PvPAttackValidateResult.Deny("That player is not in the wilderness.")
        }

        val level = wildernessLevel(attacker.coords)
        val otherLevel = wildernessLevel(target.coords)
        val minimumLevel = minOf(level, otherLevel)

        if (minimumLevel >= 1) {
            if (abs(attacker.appearance.combatLevel - target.appearance.combatLevel) > minimumLevel) {
                val pronouns =
                    when (target.vars["varbit.settings_transmit_pronouns"]) {
                        0 -> "He"
                        1 -> "She"
                        2 -> "They"
                        else -> if (target.appearance.bodyType == constants.bodytype_a) "He" else "She"
                    }
                return PvPAttackValidateResult.Deny(
                    "The difference between your Combat level and the Combat level of ${target.displayName} is too great." +
                        " $pronouns needs to move deeper into the Wilderness before you can attack them.",
                )
            }
        }

        return PvPAttackValidateResult.Pass
    }

    override fun onPlayerAttack(attacker: Player, target: Player) {
        if (!attacker.canPvp()) {
            return
        }

        if (attacker.isSkullEquipLocked()) {
            return
        }

        if (wouldSkull(attacker, target)) {
            attacker.applySkull(SkullSource.COMBAT)
        } else if (attacker.isSkulled()) {
            attacker.attr[SKULL_SOURCE] = SkullSource.COMBAT
            attacker.softTimer("timer.skull_timer", SkullSource.COMBAT.timerTicks)
            attacker.refreshSkullIcon()
        }
    }

    override fun onPlayerHit(attacker: Player, target: Player) {
        if (!attacker.canPvp()) {
            return
        }
        target.attr[LAST_PVP_HIT_TICK_ATTR] = target.currentMapClock

        if (!areaChecker.inArea("area.multiway", attacker.coords)) {
            return
        }
        if (attacker.isSkullPreventionEnabled()) {
            return
        }
        if (attacker.isSkullEquipLocked() || !wouldSkull(attacker, target)) {
            return
        }

        if (!attacker.isSkulled()) {
            attacker.applySkull(SkullSource.COMBAT)
        } else {
            attacker.attr[SKULL_SOURCE] = SkullSource.COMBAT
            attacker.softTimer("timer.skull_timer", SkullSource.COMBAT.timerTicks)
            attacker.refreshSkullIcon()
        }
    }

    private fun wouldSkull(player: Player, target: Player): Boolean {
        val targetPacked = target.uid.packed
        val playerPacked = player.uid.packed

        // Retaliating against a player who already attacked you does not skull.
        if (player.vars["varp.pk_predator1"] == targetPacked) return false
        if (player.vars["varp.pk_predator2"] == targetPacked) return false
        if (player.vars["varp.pk_predator3"] == targetPacked) return false

        if (!player.isInPvpCombat()) {
            return true
        }

        // Continuing an active fight with this target does not apply a fresh skull.
        if (player.vars["varp.pk_prey1"] == targetPacked) return false
        if (player.vars["varp.pk_prey2"] == targetPacked) return false
        if (target.vars["varp.pk_predator1"] == playerPacked) return false
        if (target.vars["varp.pk_predator2"] == playerPacked) return false

        return true
    }

    private fun wildernessLevel(tile: CoordGrid): Int {
        if (!areaChecker.inArea("area.wilderness", tile)) return -1
        val x = tile.x
        val y = tile.z
        return when (x) {
            in 2944..3392 if y in 3520..4351 -> ((y - 3520) shr 3) + 1
            in 3008..3071 if y in 10112..10175 -> ((y - 9920) shr 3) - 1
            in 2944..3455 if y in 9920..10879 -> ((y - 9920) shr 3) + 1
            in 1725..1919 if y in 11520..11583 -> 21
            in 1600..1663 if y in 11520..11583 -> 29
            else -> -1
        }
    }
}
