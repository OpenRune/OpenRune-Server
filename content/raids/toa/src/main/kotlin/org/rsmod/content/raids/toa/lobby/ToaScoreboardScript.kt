package org.rsmod.content.raids.toa.lobby

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.utils.format.formatAmount
import org.rsmod.content.raids.toa.raid.ToaKillCount
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ToaScoreboardScript : PluginScript() {
    private var Player.scoreboardTab by intVarBit(LobbyScoreboard.TAB_VARBIT)

    override fun ScriptContext.startup() {
        onOpLoc1(LobbyLocs.SCOREBOARD) { openScoreboard() }
        for ((tab, component) in LobbyScoreboard.TABS.withIndex()) {
            onIfModalButton(component) { selectTab(tab) }
        }
    }

    private fun ProtectedAccess.openScoreboard() {
        ifOpenMainModal(LobbyScoreboard.INTERFACE)
        val modes = LobbyScoreboard.MODES
        sendStats(modes.getOrElse(player.scoreboardTab) { modes.first() })
    }

    private fun ProtectedAccess.selectTab(tab: Int) {
        player.scoreboardTab = tab
        sendStats(LobbyScoreboard.MODES[tab])
    }

    private fun ProtectedAccess.sendStats(mode: String) {
        ifSetText(LobbyScoreboard.ATTEMPTS, ToaStats.attempts(player, mode).formatAmount)
        ifSetText(LobbyScoreboard.COMPLETIONS, ToaKillCount.completions(player, mode).formatAmount)
        ifSetText(LobbyScoreboard.DEATHS, ToaStats.deaths(player, mode).formatAmount)
        for (size in 1..ToaStats.MAX_TEAM_SIZE) {
            val challenge = ToaStats.bestChallenge(player, mode, size)
            val overall = ToaStats.bestOverall(player, mode, size)
            ifSetText(LobbyScoreboard.challenge(size), time(challenge))
            ifSetText(LobbyScoreboard.overall(size), time(overall))
        }
        // TODO: global columns (data_*_g*) need world-level persistence, which OpenRune lacks.
    }

    private fun time(ticks: Int): String {
        if (ticks == 0) return LobbyScoreboard.NO_TIME
        val centis = ticks * LobbyScoreboard.CENTIS_PER_TICK
        val seconds = centis / LobbyScoreboard.CENTIS_PER_SECOND
        return "%d:%02d.%02d".format(seconds / 60, seconds % 60, centis % 100)
    }
}
