package org.rsmod.content.events.demonicpacts

import jakarta.inject.Inject
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onPlayerInit
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DemonicPactsLeagueScript @Inject constructor() : PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    override fun ScriptContext.startup() {
        // Set on init so every league script's login handler already sees this league.
        onPlayerInit {
            VarPlayerIntMapSetter.set(player, "varbit.league_type", LEAGUE_TYPE)
            VarPlayerIntMapSetter.set(player, "varbit.league_type_previous", PREVIOUS_LEAGUE_TYPE)
        }
    }

    private companion object {
        const val LEAGUE_TYPE = 6
        const val PREVIOUS_LEAGUE_TYPE = 5
    }
}
