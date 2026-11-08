package org.rsmod.content.events.leaguetasks

import jakarta.inject.Inject
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onPlayerInit
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class LeagueWorldScript @Inject constructor() : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    override fun ScriptContext.startup() {
        onPlayerInit {
            VarPlayerIntMapSetter.set(player, "varp.map_flags_cached", LEAGUE_WORLD_FLAGS)
            // Stand-in for the league tutorial; the journal hides the league tab below 3.
            VarPlayerIntMapSetter.set(player, "varbit.league_tutorial_completed", TUTORIAL_COMPLETE)
        }
    }

    private companion object {
        const val LEAGUE_WORLD_FLAGS = 0x40000001
        const val TUTORIAL_COMPLETE = 14
    }
}
