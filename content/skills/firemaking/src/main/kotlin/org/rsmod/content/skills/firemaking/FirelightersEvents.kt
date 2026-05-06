package org.rsmod.content.skills.firemaking

import dev.openrune.internalName
import org.rsmod.api.player.output.mes
import org.rsmod.api.script.onOpHeldU
import org.rsmod.api.table.FiremakingColoredLogsRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class FirelightersEvents : PluginScript() {

    override fun ScriptContext.startup() {
        FiremakingColoredLogsRow.all().forEach { log ->
            onOpHeldU("obj.logs",log.firelighter) {
                val itemName = log.logItem.name.substringBefore(" ")
                invReplace(player.inv,"obj.logs",1, log.logItem.internalName)
                invDel(player.inv, log.firelighter.internalName)
                player.mes("You coat the logs with the $itemName chemicals")
            }
        }
    }

}
