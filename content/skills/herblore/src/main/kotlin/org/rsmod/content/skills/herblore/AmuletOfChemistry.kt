package org.rsmod.content.skills.herblore

import org.rsmod.api.player.front
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.random.GameRandom
import org.rsmod.api.table.herblore.HerbloreFinishedRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

object AmuletOfChemistry {
    const val PROC_CHANCE_PERCENT = 5
    const val MAX_CHARGES = 5
    const val CHARGES_VARBIT = "varbit.charges_alchemists_amulet_quantity"
    const val STOP_ON_CRUMBLE_VARBIT = "varbit.alchemy_anim_toggles"

    val AMULET_TYPES = setOf(
        "obj.amulet_of_chemistry",
        "obj.amulet_of_chemistry_imbued_charged",
        "obj.amulet_of_chemistry_imbued_uncharged"
    )

    private val WEARABLE_TYPES = setOf(
        "obj.amulet_of_chemistry",
        "obj.amulet_of_chemistry_imbued_charged"
    )

    private val ALWAYS_FOUR_DOSE_UNF_POTS = setOf("obj.torstol")
    private val DOSE_PATTERN = Regex("^obj\\.(\\d)dose(.+)$")
    private val PLUS_DOSE_PATTERN = Regex("^obj\\.(.+)\\+(\\d+)$")

    data class BrewResult(
        val output: String,
        val extraDoseApplied: Boolean,
        val crumbled: Boolean,
    )

    fun isWearing(player: Player): Boolean {
        val neck = player.front ?: return false
        return WEARABLE_TYPES.any { neck.isType(it) }
    }

    fun rollBrewOutput(player: Player, random: GameRandom, potion: HerbloreFinishedRow): BrewResult {
        val baseOutput = potion.outputPotion.internalName
        if (!isWearing(player) || player.chemistryCharges <= 0) {
            return BrewResult(baseOutput, extraDoseApplied = false, crumbled = false)
        }

        val unfPot = potion.unfPot.internalName
        if (!canApplyChemistry(unfPot, baseOutput)) {
            return BrewResult(baseOutput, extraDoseApplied = false, crumbled = false)
        }

        val upgraded = upgradeOutput(baseOutput) ?: return BrewResult(baseOutput, extraDoseApplied = false, crumbled = false)

        if (!random.randomBoolean(100 / PROC_CHANCE_PERCENT)) {
            return BrewResult(baseOutput, extraDoseApplied = false, crumbled = false)
        }

        val remaining = player.chemistryCharges - 1
        player.chemistryCharges = remaining.coerceAtLeast(0)

        val crumbled = remaining <= 0
        if (crumbled) {
            player.front = null
        }

        return BrewResult(upgraded, extraDoseApplied = true, crumbled = crumbled)
    }

    /** Parses dose count from `obj.Ndose…` or `obj.name+N` potion ids. */
    fun parseDose(internal: String): Int? {
        DOSE_PATTERN.matchEntire(internal)?.let { return it.groupValues[1].toInt() }
        PLUS_DOSE_PATTERN.matchEntire(internal)?.let { return it.groupValues[2].toInt() }
        return null
    }

    /**
     * Amulet only works when the input potion has fewer than 4 doses (stamina, divine, antivenom+, …).
     * Standard unfinished-vial brews (no dose on input) still upgrade 3-dose outputs to 4-dose.
     */
    fun canApplyChemistry(unfPot: String, output: String): Boolean {
        if (unfPot in ALWAYS_FOUR_DOSE_UNF_POTS) {
            return false
        }
        val inputDose = parseDose(unfPot)
        if (inputDose != null) {
            if (inputDose >= 4) {
                return false
            }
        } else {
            val outputDose = parseDose(output) ?: return false
            if (outputDose != 3) {
                return false
            }
        }
        return upgradeOutput(output) != null
    }

    fun upgradeOutput(output: String): String? {
        val dose = parseDose(output) ?: return null
        if (dose >= 4) {
            return null
        }
        PLUS_DOSE_PATTERN.matchEntire(output)?.let { match ->
            return "obj.${match.groupValues[1]}+${dose + 1}"
        }
        DOSE_PATTERN.matchEntire(output)?.let { match ->
            return "obj.${dose + 1}dose${match.groupValues[2]}"
        }
        return null
    }

    fun resetCharges(player: Player) {
        player.chemistryCharges = MAX_CHARGES
    }
}

private var Player.chemistryCharges by intVarBit(AmuletOfChemistry.CHARGES_VARBIT)

internal var Player.chemistryStopOnCrumble by boolVarBit(AmuletOfChemistry.STOP_ON_CRUMBLE_VARBIT)

fun Player.shouldStopBrewingOnChemistryCrumble(): Boolean = chemistryStopOnCrumble

fun Player.chemistryChargeCount(): Int = chemistryCharges.coerceIn(0, AmuletOfChemistry.MAX_CHARGES)
