package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.player.events.ResourceKind
import org.rsmod.api.script.onPlayerAttackResult
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** Special attacks and ammo, read from each finished attack. Saved ammo still counts as fired. */
class CombatTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val specialRows = progress.perLeague { it.rows(LeagueTaskTrigger.SpecialAttack) }

    private val ammoRows = progress.perLeague { it.rowsByObj(LeagueTaskTrigger.Ammo) }

    override fun ScriptContext.startup() {
        onPlayerAttackResult {
            if (special) {
                val weaponId = weapon?.id
                val met =
                    specialRows[player].orEmpty().filter {
                        it.objs.isEmpty() || weaponId in it.objs
                    }
                progress.record(player, met)
            }
            val ammoByObj = ammoRows[player] ?: return@onPlayerAttackResult
            for (spend in resources) {
                val ammo = spend.item ?: continue
                if (spend.kind != ResourceKind.Ammo) {
                    continue
                }
                val rows = ammoByObj[ammo.id] ?: continue
                progress.record(player, rows, spend.count)
            }
        }
    }
}
