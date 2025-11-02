package org.alter.skills.firemaking

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.ext.message
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.SatisfyType
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM.asRSCM

class FirelightersEvents : PluginEvent() {

    override fun init() {
        ColoredLogs.entries.forEach { coloredLog ->
            onItemOnItem(coloredLog.firelighter, "items.logs").type(SatisfyType.ANY) {
                val itemName = getItem(coloredLog.firelighter.asRSCM())?.name?.substringBefore(" ") ?: "firelighter"
                player.inventory.replace("items.logs".asRSCM(), coloredLog.logItem.asRSCM())
                player.message("You coat the logs with the $itemName chemicals")
            }
        }
    }
}