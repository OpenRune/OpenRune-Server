package org.rsmod.content.bosses.doom

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlin.reflect.KMutableProperty1
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player

private var Player.totalLevels by intVarp("varp.total_dom_levels")
private var Player.lastDelveLevel by intVarp("varp.dom_last_delve_level")
private var Player.lastLevelDuration by intVarp("varp.dom_last_level_duration")
private var Player.totalDuration by intVarp("varp.dom_total_duration")
private var Player.levelStartTime by intVarp("varp.dom_level_start_time")
private var Player.deepestLevel by intVarp("varp.dom_deepest_level")

private var Player.completions1 by intVarp("varp.dom_level_1_completions")
private var Player.completions2 by intVarp("varp.dom_level_2_completions")
private var Player.completions3 by intVarp("varp.dom_level_3_completions")
private var Player.completions4 by intVarp("varp.dom_level_4_completions")
private var Player.completions5 by intVarp("varp.dom_level_5_completions")
private var Player.completions6 by intVarp("varp.dom_level_6_completions")
private var Player.completions7 by intVarp("varp.dom_level_7_completions")
private var Player.completions8 by intVarp("varp.dom_level_8_completions")
private var Player.completions8Plus by intVarp("varp.dom_level_8_plus_completions")

private var Player.bestTime1 by intVarBit("varbit.dom_level_1_best_time")
private var Player.bestTime2 by intVarBit("varbit.dom_level_2_best_time")
private var Player.bestTime3 by intVarBit("varbit.dom_level_3_best_time")
private var Player.bestTime4 by intVarBit("varbit.dom_level_4_best_time")
private var Player.bestTime5 by intVarBit("varbit.dom_level_5_best_time")
private var Player.bestTime6 by intVarBit("varbit.dom_level_6_best_time")
private var Player.bestTime7 by intVarBit("varbit.dom_level_7_best_time")
private var Player.bestTime8 by intVarBit("varbit.dom_level_8_best_time")
private var Player.bestTime8Plus by intVarBit("varbit.dom_level_8_plus_best_time")

private val COMPLETIONS: List<KMutableProperty1<Player, Int>> =
    listOf(
        Player::completions1,
        Player::completions2,
        Player::completions3,
        Player::completions4,
        Player::completions5,
        Player::completions6,
        Player::completions7,
        Player::completions8,
        Player::completions8Plus,
    )

private val BEST_TIMES: List<KMutableProperty1<Player, Int>> =
    listOf(
        Player::bestTime1,
        Player::bestTime2,
        Player::bestTime3,
        Player::bestTime4,
        Player::bestTime5,
        Player::bestTime6,
        Player::bestTime7,
        Player::bestTime8,
        Player::bestTime8Plus,
    )

@Singleton
internal class DoomStats @Inject constructor(private val clock: MapClock) {
    fun startRun(player: Player) {
        player.totalDuration = 0
    }

    fun startLevel(player: Player) {
        player.levelStartTime = clock.cycle
    }

    fun complete(player: Player, level: Int) {
        val duration = clock.cycle - player.levelStartTime
        val deep = level >= DoomDelve.DEEP_LEVEL
        val index = minOf(level, DoomDelve.DEEP_LEVEL) - 1
        val previousBest = BEST_TIMES[index].get(player)
        val newBest = previousBest == 0 || duration < previousBest

        player.totalLevels += 1
        COMPLETIONS[index].set(player, COMPLETIONS[index].get(player) + 1)
        player.lastDelveLevel = index
        player.lastLevelDuration = duration
        player.totalDuration += duration
        if (level > player.deepestLevel) player.deepestLevel = level
        if (newBest) BEST_TIMES[index].set(player, duration)

        val suffix =
            if (newBest) " (new personal best)" else ". Personal best: ${highlight(previousBest)}"
        val label = if (deep) "${DoomDelve.DEEPEST}+" else level.toString()
        player.mes("Delve level: $label duration: ${highlight(duration)}$suffix")
        if (level == DoomDelve.DEEPEST) {
            player.mes("Delve level 1 - $level duration: ${highlight(player.totalDuration)}")
        } else {
            player.mes("Total duration: ${highlight(player.totalDuration)}")
        }
        if (deep) player.mes("Deep delves completed: @mes_hl_red@${COMPLETIONS[index].get(player)}</col>")
    }

    private fun highlight(ticks: Int): String = "@mes_hl_red@${format(ticks)}</col>"

    private fun format(ticks: Int): String {
        val centis = ticks * TICK_CENTIS
        return "%d:%02d.%02d".format(centis / 6000, centis / 100 % 60, centis % 100)
    }

    private companion object {
        private const val TICK_CENTIS = 60
    }
}
