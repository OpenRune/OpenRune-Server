package org.rsmod.api.instances.timer

import org.rsmod.api.instances.INSTANCE_KILL_TIMER_MAX_PLAYERS
import org.rsmod.api.instances.INSTANCE_KILL_TIMER_MAX_TICKS
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.instanceKillBest
import org.rsmod.api.instances.recordInstanceKillBest
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.game.entity.Player

internal object InstanceKillTimer {

    fun tracksKillTime(session: InstanceSession): Boolean =
        session.spec.maxPlayers <= INSTANCE_KILL_TIMER_MAX_PLAYERS

    /**
     * Jagex's timer format: `m:ss.hh` with the precise timing setting on, `m:ss` without.
     * Live capture, rev 241, 20261012T180256-df49f48@1610-1947 (precise timing on).
     */
    fun formatDuration(ticks: Int, precise: Boolean): String {
        val centis = ticks * TICK_CENTIS
        val minutes = centis / 6000
        val seconds = centis / 100 % 60
        // UNVERIFIED: whole seconds without precise timing, rounded down (wiki: Settings).
        return if (precise) {
            "%d:%02d.%02d".format(minutes, seconds, centis % 100)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    fun reportKillTime(player: Player, instanceKey: String, elapsedTicks: Int) {
        val precise = player.vars["varbit.option_precise_timing"] != 0
        val duration = formatDuration(elapsedTicks, precise)

        if (elapsedTicks > INSTANCE_KILL_TIMER_MAX_TICKS) {
            player.mes(
                "<col=ff0000>Your kill time of $duration was too long to record a personal best.</col>",
                ChatType.Engine,
            )
            return
        }

        val improved = player.recordInstanceKillBest(instanceKey, elapsedTicks)
        val best = player.instanceKillBest(instanceKey)?.let { formatDuration(it, precise) }

        // live capture, rev 241, 20261012T180256-df49f48@1610 (new best) and @1745
        val message =
            when {
                improved -> "Fight duration: <col=ff0000>$duration</col> (new personal best)"
                best != null -> "Fight duration: <col=ff0000>$duration</col>. Personal best: $best"
                else -> "Fight duration: <col=ff0000>$duration</col>."
            }
        player.mes(message, ChatType.Engine)
    }

    private const val TICK_CENTIS = 60
}
