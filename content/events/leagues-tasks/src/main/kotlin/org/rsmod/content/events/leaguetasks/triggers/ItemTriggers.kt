package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerInvChanged
import org.rsmod.api.script.onPlayerShopBought
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskRequirement
import org.rsmod.content.events.leaguetasks.pack.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ItemTriggers @Inject constructor(private val progress: LeagueTaskProgress) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val obtainRows: Map<Int, List<LeagueTaskRequirement>> =
        progress.rowsByObj(LeagueTaskTrigger.Obtain)

    private val buyRows: Map<Int, List<LeagueTaskRequirement>> =
        progress.rowsByObj(LeagueTaskTrigger.ShopBuy)

    override fun ScriptContext.startup() {
        onPlayerInvChanged {
            if (obtainRows.isEmpty()) {
                return@onPlayerInvChanged
            }
            for ((obj, amount) in acquired) {
                val rows = obtainRows[obj] ?: continue
                progress.record(player, rows, amount)
            }
        }

        onPlayerShopBought {
            val rows = buyRows[item.id] ?: return@onPlayerShopBought
            progress.record(player, rows, count)
        }
    }
}
