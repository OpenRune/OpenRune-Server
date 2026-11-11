package org.rsmod.content.events.leaguetasks.triggers

import jakarta.inject.Inject
import org.rsmod.api.script.onPlayerInvChanged
import org.rsmod.api.script.onPlayerShopBought
import org.rsmod.content.events.leaguetasks.LeagueTaskProgress
import org.rsmod.content.events.leaguetasks.LeagueTaskTrigger
import org.rsmod.game.world.WorldType
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class ItemTriggers @Inject constructor(private val progress: LeagueTaskProgress) : PluginScript() {
    override val worldTypes = WorldType.entries.filter { it.isLeague }

    private val obtainRows = progress.perLeague { it.rowsByObj(LeagueTaskTrigger.Obtain) }

    private val buyRows = progress.perLeague { it.rowsByObj(LeagueTaskTrigger.ShopBuy) }

    override fun ScriptContext.startup() {
        onPlayerInvChanged {
            val byObj = obtainRows[player]
            if (byObj.isNullOrEmpty()) {
                return@onPlayerInvChanged
            }
            for ((obj, amount) in acquired) {
                val rows = byObj[obj] ?: continue
                progress.record(player, rows, amount)
            }
        }

        onPlayerShopBought {
            val rows = buyRows[player]?.get(item.id) ?: return@onPlayerShopBought
            progress.record(player, rows, count)
        }
    }
}
