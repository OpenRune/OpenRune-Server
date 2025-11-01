package org.alter.plugins.content.interfaces.bank

import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.bank.config.Varbits

class CommandsPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("obank", Privilege.ADMIN_POWER) {
            player.openBank()
        }

        /**
         * Clears all bank tab varbits for the player, effectively
         * dumping all items back into the one main tab.
         */
        onCommand("tabreset") {
            for (tab in 1..9)
                player.setVarbit("varbits.bank_tab_display" + tab, 0)
            player.setVarbit("varbits.bank_currenttab", 0)
        }
    }
}
