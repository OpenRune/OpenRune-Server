package org.rsmod.content.other.mapclock

import jakarta.inject.Inject
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.player.vars.intVarp
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerSoftTimer
import org.rsmod.game.MapClock
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext
import java.time.Instant
import java.time.temporal.ChronoUnit

class MapClockScript @Inject constructor(private val clock: MapClock) : PluginScript() {
    private var Player.playtime by intVarp("varp.playtime")
    private var Player.mapClock by intVarp("varp.map_clock")
    private var Player.msPastMinute by intVarBit("varbit.date_milliseconds_past_minute")
    private var Player.secsPastMinute by intVarBit("varbit.date_seconds_past_minute")
    private var Player.runeday by intVarBit("varbit.current_runeday")

    override fun ScriptContext.startup() {
        onPlayerLogin {
            player.initClockTimer()
            player.runeday = currentRuneday()
        }
        onPlayerSoftTimer("timer.map_clock") { player.incrementClock() }
    }

    private fun Player.initClockTimer() {
        softTimer("timer.map_clock", 1)
    }

    private fun Player.incrementClock() {
        val now = System.currentTimeMillis()
        msPastMinute = (now % 1000).toInt()
        secsPastMinute = ((now / 1000) % 60).toInt()
        mapClock = clock.cycle
        playtime++
    }

    private fun currentRuneday(): Int =
        ChronoUnit.DAYS.between(RUNEDATE_EPOCH, Instant.now()).toInt()

    private companion object {
        val RUNEDATE_EPOCH: Instant = Instant.parse("2002-02-27T00:00:00Z")
    }
}
