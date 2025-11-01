package org.alter.skills.firemaking

import dev.openrune.ServerCacheManager.getItem
import org.alter.api.ext.message
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.SatisfyType
import org.alter.game.pluginnew.event.impl.onItemOnItem
import org.alter.rscm.RSCM.asRSCM
import org.alter.skills.firemaking.BurnLogEvents.Companion.COLOURED_LOGS

class FirelightersEvents : PluginEvent() {

    override fun init() {
        COLOURED_LOGS.forEach {
            val lighter = it.value.first
            onItemOnItem(lighter, "items.logs").type(SatisfyType.ANY) {
                val item = getItem(lighter.asRSCM())!!.name.substringBefore(" ")
                player.inventory.replace("items.logs".asRSCM(),it.key)
                player.message("You coat the logs with the $item chemicals")
            }


        }
    }

}