package org.rsmod.api.instances.timer

import org.rsmod.api.instances.INSTANCE_KILL_TIMER_MAX_PLAYERS
import org.rsmod.api.instances.INSTANCE_KILL_TIMER_MAX_TICKS
import org.rsmod.api.instances.InstanceSession
import org.rsmod.api.instances.instanceKillBest
import org.rsmod.api.instances.recordInstanceKillBest
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/** A kill time in Jagex's timer format, as the fight duration message shows it. */
public fun formatInstanceKillTime(ticks: Int, precise: Boolean): String =
    InstanceKillTimer.formatDuration(ticks, precise)

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

    /** Reports [elapsedTicks] to [player] and returns whether it is a new personal best. */
    fun reportKillTime(player: Player, session: InstanceSession, elapsedTicks: Int): Boolean {
        val precise = player.vars["varbit.option_precise_timing"] != 0
        val duration = formatDuration(elapsedTicks, precise)

        if (elapsedTicks > INSTANCE_KILL_TIMER_MAX_TICKS) {
            player.mes(
                "<col=ff0000>Your kill time of $duration was too long to record a personal best.</col>",
                ChatType.Engine,
            )
            return false
        }

        val varp = session.spec.bestTimeVarp
        val improved =
            if (varp == null) {
                player.recordInstanceKillBest(session.key, elapsedTicks)
            } else {
                val previous = player.vars[varp]
                (previous == 0 || elapsedTicks < previous).also {
                    if (it) VarPlayerIntMapSetter.set(player, varp, elapsedTicks)
                }
            }
        val bestTicks = if (varp == null) player.instanceKillBest(session.key) else player.vars[varp]
        val best = bestTicks?.let { formatDuration(it, precise) }

        // live capture, rev 241, 20261012T180256-df49f48@1610 (new best) and @1745
        val message =
            when {
                improved -> "Fight duration: <col=ff0000>$duration</col> (new personal best)"
                best != null -> "Fight duration: <col=ff0000>$duration</col>. Personal best: $best"
                else -> "Fight duration: <col=ff0000>$duration</col>."
            }
        player.mes(message, ChatType.Engine)
        return improved
    }

    private const val TICK_CENTIS = 60
}
