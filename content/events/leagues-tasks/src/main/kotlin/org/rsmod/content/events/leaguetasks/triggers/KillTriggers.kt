package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.script.onNpcKilled
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Kills count for the player the drops and kill count go to. */
class KillTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rows = progress.rows(LeagueTaskTrigger.Kill)

    private val rowsByNpc: Map<Int, List<LeagueTaskRequirement>> =
        rows.flatMap { row -> row.npcs.map { it to row } }.groupBy({ it.first }, { it.second })

    private val rowsByCategory: Map<Int, List<LeagueTaskRequirement>> =
        rows.flatMap { row -> row.categories.map { it to row } }
            .groupBy({ it.first }, { it.second })

    private val slayerRows: List<LeagueTaskRequirement> = rows.filter { it.slayerTask != null }

    override fun ScriptContext.startup() {
        onNpcKilled {
            if (!isHero) {
                return@onNpcKilled
            }
            progress.record(player, matching(npc.type) + matching(npc.visType))
        }
    }

    private fun matching(type: NpcServerType): Set<LeagueTaskRequirement> = buildSet {
        addAll(rowsByNpc[type.id].orEmpty())
        addAll(rowsByCategory[type.category].orEmpty())
        slayerRows.filterTo(this) { SlayerTaskCredit.counts(type, it.slayerTask ?: 0) }
    }
}
