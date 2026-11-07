package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerEquip
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class EquipmentTriggers @Inject constructor(private val progress: LeagueTaskProgress) :
    PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val rowsByObj: Map<Int, List<LeagueTaskRequirement>> =
        progress.rowsByObj(LeagueTaskTrigger.Equip)

    override fun ScriptContext.startup() {
        onPlayerEquip {
            for (row in rowsByObj[item.id].orEmpty()) {
                progress.check(player, row.task)
            }
        }
    }
}
