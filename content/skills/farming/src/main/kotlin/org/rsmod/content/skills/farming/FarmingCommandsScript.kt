package org.rsmod.content.skills.farming

import dev.or2.central.account.Rights
import jakarta.inject.Inject
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onCommand
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.game.cheat.Cheat
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FarmingCommandsScript @Inject constructor(
    private val config: ServerConfig,
    private val farming: FarmingService,
) : PluginScript() {
    override fun ScriptContext.startup() {
        FarmingClock.defaultRate = config.gameplay.farmingGrowthRate
        onCommand("farmrate") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Show or set your Farming growth speed"
            invalidArgs = "Usage: ::farmrate [positive integer|reset]"
            cheat { rate() }
        }
        onCommand("farmadvance") {
            requiredRights = Rights.ADMINISTRATOR
            desc = "Advance your Farming clock through normal growth and disease checks"
            invalidArgs = "Usage: ::farmadvance minutes"
            cheat { advance() }
        }
    }

    private fun Cheat.rate() {
        if (args.size > 1) return player.mes("Usage: ::farmrate [positive integer|reset]")
        val value = args.firstOrNull()
        if (value != null) {
            val rate = if (value.equals("reset", ignoreCase = true)) 0 else value.toIntOrNull()
            if (rate == null || rate > FarmingClock.MAX_RATE ||
                (rate <= 0 && !value.equals("reset", ignoreCase = true))) {
                return player.mes("Use a rate from 1 to ${FarmingClock.MAX_RATE} or reset for the server default.")
            }
            refresh()
            player.farmingGrowthRate = rate
        }
        player.mes("Farming growth: ${FarmingClock.rate(player)}x (server ${FarmingClock.defaultRate}x, " +
            "player ${player.farmingGrowthRate.takeIf { it > 0 }?.let { "${it}x" } ?: "default"}).")
    }

    private fun Cheat.advance() {
        val minutes = args.singleOrNull()?.toIntOrNull()
        if (minutes == null || minutes <= 0) return player.mes("Usage: ::farmadvance positiveMinutes")
        val millis = System.currentTimeMillis()
        val current = FarmingClock.millis(player, millis) / FarmingClock.MINUTE_MILLIS
        if (minutes.toLong() + current > FarmingClock.MAX_ADVANCE_MINUTE) {
            return player.mes("That advance leaves insufficient Farming clock headroom.")
        }
        refresh(millis)
        FarmingClock.advance(player, minutes, millis)
        refresh(millis)
        player.mes("Advanced Farming by $minutes growth minutes, including normal disease checks.")
    }

    private fun Cheat.refresh(millis: Long = System.currentTimeMillis()) {
        farming.refresh(player, millis)
        FarmingSaplings.grow(player, millis)
    }
}
