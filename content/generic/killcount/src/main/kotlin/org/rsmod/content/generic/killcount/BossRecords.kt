package org.rsmod.content.generic.killcount

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

public object BossRecords {
    public fun bestTicks(player: Player, killcountVarp: Int): Int =
        player.attr.getOrDefault(bestKey(killcountVarp), 0)

    public fun recordBest(player: Player, killcountVarp: Int, ticks: Int): Boolean {
        require(ticks > 0)
        val previous = bestTicks(player, killcountVarp)
        if (previous > 0 && ticks >= previous) return false
        player.attr[bestKey(killcountVarp)] = ticks
        return true
    }

    public fun formatTime(ticks: Int, precise: Boolean, separateHours: Boolean): String {
        val tenths = ticks.toLong() * 6
        val totalSeconds = if (precise) tenths / 10 else (tenths + 5) / 10
        val hours = totalSeconds / 3600
        val minutes = if (separateHours) totalSeconds / 60 else totalSeconds / 60 % 60
        val seconds = (totalSeconds % 60).toString().padStart(2, '0')
        val prefix = if (!separateHours && hours > 0) "$hours:" else ""
        val minuteText = if (hours > 0) minutes.toString().padStart(2, '0') else minutes.toString()
        val fraction = if (precise) ".${tenths % 10}0" else ""
        return "$prefix$minuteText:$seconds$fraction"
    }

    private fun bestKey(varp: Int): AttributeKey<Int> = AttributeKey("boss_best_ticks:$varp")
}
