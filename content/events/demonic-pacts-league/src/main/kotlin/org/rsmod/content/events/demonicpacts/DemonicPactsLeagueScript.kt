package org.rsmod.content.events.demonicpacts

import jakarta.inject.Inject
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onPlayerInit
import org.rsmod.api.table.RegionDataRow
import org.rsmod.content.events.leaguetasks.LeagueTaskDefinitions
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DemonicPactsLeagueScript @Inject constructor(definitions: LeagueTaskDefinitions) :
    PluginScript() {
    override val worldTypes = listOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val leagueType = definitions.typeOf(WorldType.DEMONIC_PACTS_LEAGUE)

    private val startingRegion = RegionDataRow.getRow("dbrow.region_varlamore").regionId

    override fun ScriptContext.startup() {
        // Set on init so every league script's login handler already sees this league.
        onPlayerInit {
            VarPlayerIntMapSetter.set(player, "varbit.league_type", leagueType)
            VarPlayerIntMapSetter.set(player, "varbit.league_type_previous", PREVIOUS_LEAGUE_TYPE)
            // Stand-in for the tutorial, which assigns the forced first region
            // (`[proc,league_force_region_choice]`).
            if (player.vars["varbit.league_area_selection_0"] == 0) {
                VarPlayerIntMapSetter.set(player, "varbit.league_area_selection_0", startingRegion)
            }
        }
    }

    private companion object {
        const val PREVIOUS_LEAGUE_TYPE = 5
    }
}
