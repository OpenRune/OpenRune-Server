package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import jakarta.inject.Inject
import org.rsmod.api.player.vars.PlayerVarChangedEvent
import org.rsmod.api.player.vars.PlayerVarWatch
import org.rsmod.api.script.onEvent
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Re-checks collection log slot tasks whenever the log's total count changes. */
class CollectionTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val countVarp = "varp.collection_count".asRSCM(RSCMType.VARP)

    override fun ScriptContext.startup() {
        PlayerVarWatch.watch(countVarp)
        onEvent<PlayerVarChangedEvent> {
            if (varp == countVarp) {
                progress.checkAll(player, LeagueTaskTrigger.CollectionSlots)
            }
        }
    }
}
