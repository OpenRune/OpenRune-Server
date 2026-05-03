package org.rsmod.api.mechanics.toxins

import org.rsmod.api.mechanics.toxins.impl.PlayerDisease
import org.rsmod.api.mechanics.toxins.impl.PlayerPoison
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom
import org.rsmod.api.mechanics.toxins.impl.PlayerVenom.TICK_INTERVAL
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.script.onPlayerTimer
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

public class PlayerPoisonTimerScript : PluginScript() {
    override fun ScriptContext.startup() {
        onPlayerLogin {
            if (player.vars["varp.poison_severity"] > 0) {
                player.timer("timer.player_poison", PlayerPoison.TICK_INTERVAL)
            }

            if (player.vars["varp.venom_strikes"] > 0) {
                player.timer("timer.player_venom", TICK_INTERVAL)
            }
            PlayerDisease.rearmTimerAfterLogin(player)
            Toxin.syncStatusOrbs(player)
        }
        onPlayerTimer("timer.player_poison") { PlayerPoison.onPoisonTimerTick(player) }
        onPlayerTimer("timer.player_venom") { PlayerVenom.onVenomTimerTick(player) }
        onPlayerTimer("timer.player_disease") { PlayerDisease.onDiseaseTimerTick(player) }
    }
}
