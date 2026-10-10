package org.rsmod.content.skills.thieving

import org.rsmod.api.invtx.invDel
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.boolVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.isType

private var Player.glovesOfSilenceFailures by intVarp("varp.gloves_of_silence_failures")
private var Player.dodgyNecklaceUses by intVarp("varp.dodgy_necklace_uses")
private val Player.ardougneMediumDiary by boolVarBit("varbit.ardougne_diary_medium_complete")
private val Player.ardougneHardDiary by boolVarBit("varbit.ardougne_diary_hard_complete")
private val Player.ardougneEliteDiary by boolVarBit("varbit.ardougne_diary_elite_complete")

internal data class PickpocketRates(val low: Int, val high: Int, val glovesApplied: Boolean)

internal object ThievingEquipment {
    const val GLOVES_OF_SILENCE = "obj.hunting_silent_gloves"
    const val DODGY_NECKLACE = "obj.dodgy_necklace"
    private const val GLOVES_LIFESPAN = 62
    private const val DODGY_CHARGES = 10
    private const val DODGY_CHANCE = 4
    private const val ROGUE_PIECE_CHANCE = 15

    private val THIEVING_CAPES = listOf("obj.skillcape_thieving", "obj.skillcape_thieving_trimmed")
    private val ROGUE_PIECES =
        listOf(
            "obj.roguesden_helm",
            "obj.roguesden_body",
            "obj.roguesden_legs",
            "obj.roguesden_gloves",
            "obj.roguesden_boots",
        )

    fun rates(player: Player, low: Int, high: Int): PickpocketRates {
        val gloves = GLOVES_OF_SILENCE in player.worn && !player.ardougneHardDiary
        var multiplier = 1.0
        if (gloves) multiplier *= 1.05
        if (player.ardougneHardDiary) multiplier *= 1.1
        if (THIEVING_CAPES.any { it in player.worn }) multiplier *= 1.1
        return PickpocketRates((low * multiplier).toInt(), (high * multiplier).toInt(), gloves)
    }

    fun pouchLimit(player: Player): Int =
        when {
            player.ardougneEliteDiary -> 140
            player.ardougneHardDiary -> 84
            player.ardougneMediumDiary -> 56
            else -> 28
        }

    fun rogueDoubleChance(player: Player): Int {
        val pieces = ROGUE_PIECES.count { it in player.worn }
        return if (pieces == ROGUE_PIECES.size) 100 else pieces * ROGUE_PIECE_CHANCE
    }

    fun ProtectedAccess.degradeGlovesOfSilence() {
        player.glovesOfSilenceFailures++
        val remaining = GLOVES_LIFESPAN - player.glovesOfSilenceFailures
        if (remaining == 1) {
            mes("Your gloves of silence are going to fall apart!")
            return
        }
        if (remaining > 0) return
        player.glovesOfSilenceFailures = 0
        removeWorn(GLOVES_OF_SILENCE)
        mes("Your gloves of silence have fallen apart.")
    }

    fun glovesCondition(player: Player): String =
        when (GLOVES_LIFESPAN - player.glovesOfSilenceFailures) {
            GLOVES_LIFESPAN -> "Your gloves are new."
            in 34 until GLOVES_LIFESPAN -> "Your gloves are in good condition."
            in 20..33 -> "Your gloves are starting to look quite shabby."
            in 10..19 -> "Your gloves are starting to need repair."
            in 2..9 -> "Your gloves are in need of repair!"
            else -> "Your gloves are about to fall apart!"
        }

    fun ProtectedAccess.tryDodgyNecklace(): Boolean {
        if (DODGY_NECKLACE !in player.worn || random.of(DODGY_CHANCE) != 0) return false
        player.dodgyNecklaceUses++
        val remaining = DODGY_CHARGES - player.dodgyNecklaceUses
        if (remaining > 0) {
            mes("Your dodgy necklace protects you. It has ${charges(remaining)} left.")
            return true
        }
        player.dodgyNecklaceUses = 0
        removeWorn(DODGY_NECKLACE)
        mes("Your dodgy necklace protects you. It then crumbles to dust.")
        return true
    }

    fun dodgyCondition(player: Player): String =
        "Your dodgy necklace has ${charges(DODGY_CHARGES - player.dodgyNecklaceUses)} left."

    private fun charges(count: Int): String = if (count == 1) "1 charge" else "$count charges"

    private fun ProtectedAccess.removeWorn(obj: String) {
        val slot = player.worn.indexOfFirst { it != null && it.isType(obj) }
        if (slot >= 0) {
            invDel(worn, obj, 1, slot = slot)
        }
    }
}
