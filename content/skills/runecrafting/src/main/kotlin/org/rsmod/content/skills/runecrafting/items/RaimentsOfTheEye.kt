package org.rsmod.content.skills.runecrafting.items

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.entity.Player

object RaimentsOfTheEye {
    private val hatVariants =
        setOf(
            "obj.hat_of_the_eye",
            "obj.hat_of_the_eye_blue",
            "obj.hat_of_the_eye_green",
        )

    private val topVariants =
        setOf(
            "obj.robe_top_of_the_eye",
            "obj.robe_top_of_the_eye_blue",
            "obj.robe_top_of_the_eye_green",
        )

    private val bottomVariants =
        setOf(
            "obj.robe_bottom_of_the_eye",
            "obj.robe_bottom_of_the_eye_blue",
            "obj.robe_bottom_of_the_eye_green",
        )

    private val bootVariants = setOf("obj.boots_of_the_eye")

    fun pieceCount(player: Player): Int {
        val slots = listOf(hatVariants, topVariants, bottomVariants, bootVariants)
        return slots.count { variants -> variants.any { it in player.worn } }
    }

    /**
     * Each piece grants 10% extra runes; the full set adds another 20% (multiplier 6 on tens).
     * Bonus runes do not grant additional experience.
     */
    fun ProtectedAccess.applyBonus(baseRunes: Int): Int {
        val pieces = pieceCount(player)
        if (pieces == 0 || baseRunes <= 0) {
            return baseRunes
        }

        val multiplier =
            when (pieces) {
                1 -> 1
                2 -> 2
                3 -> 3
                4 -> 6
                else -> 0
            }

        val tens = baseRunes / 10
        val remainder = baseRunes % 10
        var bonus = tens * multiplier
        if (remainder > 0 && random.of(10) < remainder) {
            bonus += multiplier
        }
        return baseRunes + bonus
    }
}
