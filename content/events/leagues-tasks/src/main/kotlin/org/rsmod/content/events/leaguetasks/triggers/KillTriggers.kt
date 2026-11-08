package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.script.onNpcKilled
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.hit.Hit
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Kills count for the player the drops and kill count go to. A row with `tool` items needs the
 * killing hit to come from one of them, as the weapon or as the spell or ammo it used.
 */
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

    private val anyNpcRows: List<LeagueTaskRequirement> =
        rows.filter { it.npcs.isEmpty() && it.categories.isEmpty() && it.slayerTask == null }

    override fun ScriptContext.startup() {
        onNpcKilled {
            if (!isHero) {
                return@onNpcKilled
            }
            val hit = killingHit
            val met = (matching(npc.type) + matching(npc.visType) + anyNpcRows).filter { row ->
                row.tools.isEmpty() || hit != null && hit.usedAny(row.tools)
            }
            progress.record(player, met)
        }
    }

    private fun matching(type: NpcServerType): Set<LeagueTaskRequirement> = buildSet {
        addAll(rowsByNpc[type.id].orEmpty())
        addAll(rowsByCategory[type.category].orEmpty())
        slayerRows.filterTo(this) { SlayerTaskCredit.counts(type, it.slayerTask ?: 0) }
    }

    private fun Hit.usedAny(tools: List<Int>): Boolean {
        val weapon = righthandType()?.id
        val secondary = secondaryType()?.id
        return weapon in tools || secondary in tools
    }
}
