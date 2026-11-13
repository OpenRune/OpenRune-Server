package org.rsmod.content.skills.agility

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.table.agility.AgilityCoursesRow
import org.rsmod.game.entity.Player

object LapTimes {
    fun start(player: Player) {
        player.lapStart = player.currentMapClock
    }

    fun finish(access: ProtectedAccess, course: AgilityCoursesRow) {
        val varp = course.lapBest?.let { RSCM.getReverseMapping(RSCMType.VARP, it) } ?: return
        val style = course.lapTimeStyle
        val colour = style[0].toString(16).padStart(6, '0')
        val decimals = style[1]
        val player = access.player
        val ticks = player.currentMapClock - player.lapStart
        val best = player.vars[varp]
        val time = format(ticks, decimals)
        // UNVERIFIED: that a lap equal to the personal best is not a new one.
        if (best == 0 || ticks < best) {
            VarPlayerIntMapSetter.set(player, varp, ticks)
            access.mes("Lap duration: <col=$colour>$time</col> (new personal best)")
            return
        }
        val personalBest = format(best, decimals)
        access.mes("Lap duration: <col=$colour>$time</col>. Personal best: $personalBest")
    }

    fun format(ticks: Int, decimals: Int): String {
        val centis = ticks.toLong() * CENTIS_PER_TICK
        val minutes = centis / CENTIS_PER_MINUTE
        val seconds = centis % CENTIS_PER_MINUTE / CENTIS_PER_SECOND
        val whole = "$minutes:${seconds.toString().padStart(2, '0')}"
        if (decimals == 0) {
            return whole
        }
        val hundredths = centis % CENTIS_PER_SECOND
        return "$whole.${hundredths.toString().padStart(2, '0')}"
    }

    private const val CENTIS_PER_TICK = 60L
    private const val CENTIS_PER_SECOND = 100L
    private const val CENTIS_PER_MINUTE = 6000L

    private var Player.lapStart by intVarp("varp.agility_lap_start")
}
