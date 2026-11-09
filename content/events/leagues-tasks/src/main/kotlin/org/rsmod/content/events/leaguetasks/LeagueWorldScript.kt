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
        }
    }

    private companion object {
        const val LEAGUE_WORLD_FLAGS = 0x40000001
    }
}
