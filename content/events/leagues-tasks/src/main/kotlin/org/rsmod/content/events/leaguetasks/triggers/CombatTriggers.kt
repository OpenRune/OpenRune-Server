package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.events.ResourceKind
import org.rsmod.api.script.onPlayerAttackResult
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Special attacks and ammo, read from each finished attack. Saved ammo still counts as fired. */
class CombatTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val specialRows: List<LeagueTaskRequirement> =
        progress.rows(LeagueTaskTrigger.SpecialAttack)

    private val ammoRows: Map<Int, List<LeagueTaskRequirement>> =
        progress.rowsByObj(LeagueTaskTrigger.Ammo)

    override fun ScriptContext.startup() {
        onPlayerAttackResult {
            if (special) {
                val weaponId = weapon?.id
                val met = specialRows.filter { it.objs.isEmpty() || weaponId in it.objs }
                progress.record(player, met)
            }
            for (spend in resources) {
                val ammo = spend.item ?: continue
                if (spend.kind != ResourceKind.Ammo) {
                    continue
                }
                val rows = ammoRows[ammo.id] ?: continue
                progress.record(player, rows, spend.count)
            }
        }
    }
}
