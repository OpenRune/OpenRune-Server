package org.rsmod.content.raids.toa.lobby

import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

internal object ToaStats {
    const val MAX_TEAM_SIZE = 8

    fun recordAttempt(player: Player, mode: String) = increment(player, attemptsVarp(mode))

    fun recordDeath(player: Player, mode: String) = increment(player, deathsVarp(mode))

    fun recordTimes(
        player: Player,
        mode: String,
        teamSize: Int,
        challengeTicks: Int,
        overallTicks: Int,
    ) {
        val size = teamSize.coerceIn(1, MAX_TEAM_SIZE)
        improve(player, bestVarp(CHALLENGE, mode, size), challengeTicks)
        improve(player, bestVarp(OVERALL, mode, size), overallTicks)
    }

    fun attempts(player: Player, mode: String): Int = player.vars[attemptsVarp(mode)]

    fun deaths(player: Player, mode: String): Int = player.vars[deathsVarp(mode)]

    fun bestChallenge(player: Player, mode: String, size: Int): Int =
        player.vars[bestVarp(CHALLENGE, mode, size)]

    fun bestOverall(player: Player, mode: String, size: Int): Int =
        player.vars[bestVarp(OVERALL, mode, size)]

    private fun increment(player: Player, varp: String) {
        VarPlayerIntMapSetter.set(player, varp, player.vars[varp] + 1)
    }

    private fun improve(player: Player, varp: String, ticks: Int) {
        val best = player.vars[varp]
        if (best == 0 || ticks < best) VarPlayerIntMapSetter.set(player, varp, ticks)
    }

    private fun attemptsVarp(mode: String): String = "varp.toa_attempts_${mode.lowercase()}"

    private fun deathsVarp(mode: String): String = "varp.toa_deaths_${mode.lowercase()}"

    private fun bestVarp(kind: String, mode: String, size: Int): String =
        "varp.toa_best_${kind}_${mode.lowercase()}_$size"

    private const val CHALLENGE = "challenge"
    private const val OVERALL = "overall"
}
