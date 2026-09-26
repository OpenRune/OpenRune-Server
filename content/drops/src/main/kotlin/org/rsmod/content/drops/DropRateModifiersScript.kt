package org.rsmod.content.drops

import jakarta.inject.Inject
import org.rsmod.api.droptable.DropRateModifiers
import org.rsmod.api.server.config.ServerConfig
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DropRateModifiersScript @Inject constructor(private val config: ServerConfig) :
    PluginScript() {
    override fun ScriptContext.startup() {
        val server = config.gameplay.dropRates.multiplier
        require(server > 0.0) { "gameplay.drop-rates.multiplier must be positive, was $server" }
        DropRateModifiers.serverMultiplier = server
        DropRateModifiers.playerMultiplier = { player ->
            val percent = player.vars[PLAYER_MULTIPLIER_VARP]
            if (percent == 0) 1.0 else percent / 100.0
        }
        DropRateModifiers.install()
    }

    companion object {
        const val PLAYER_MULTIPLIER_VARP = "varp.drop_rate_multiplier"
    }
}
