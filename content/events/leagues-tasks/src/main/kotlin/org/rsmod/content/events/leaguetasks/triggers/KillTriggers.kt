package org.rsmod.content.events.leaguetasks.triggers

import dev.openrune.types.NpcServerType
import jakarta.inject.Inject
import org.rsmod.api.config.SlayerTaskCredit
import org.rsmod.api.script.onNpcKilled
import org.rsmod.content.events.leaguetasks.LeagueTaskIndex
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
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

    private val kills = progress.perLeague(::KillRows)

    override fun ScriptContext.startup() {
        onNpcKilled {
            if (!isHero) {
                return@onNpcKilled
            }
            val rows = kills[player] ?: return@onNpcKilled
            val hit = killingHit
            val met =
                (rows.matching(npc.type) + rows.matching(npc.visType) + rows.anyNpc).filter { row ->
                    row.tools.isEmpty() || hit != null && hit.usedAny(row.tools)
                }
            progress.record(player, met)
        }
    }

    private class KillRows(index: LeagueTaskIndex) {
        private val rows = index.rows(LeagueTaskTrigger.Kill)

        private val byNpc = index.rowsBy(LeagueTaskTrigger.Kill) { it.npcs }

        private val byCategory = index.rowsBy(LeagueTaskTrigger.Kill) { it.categories }

        private val slayer: List<LeagueTaskRequirement> = rows.filter { it.slayerTask != null }

        val anyNpc: List<LeagueTaskRequirement> =
            rows.filter { it.npcs.isEmpty() && it.categories.isEmpty() && it.slayerTask == null }

        fun matching(type: NpcServerType): Set<LeagueTaskRequirement> = buildSet {
            addAll(byNpc[type.id].orEmpty())
            addAll(byCategory[type.category].orEmpty())
            slayer.filterTo(this) { SlayerTaskCredit.counts(type, it.slayerTask ?: 0) }
        }
    }

    private fun Hit.usedAny(tools: List<Int>): Boolean {
        val weapon = righthandType()?.id
        val secondary = secondaryType()?.id
        return weapon in tools || secondary in tools
    }
}
