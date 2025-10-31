package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.*

class EmptybankPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("emptybank", Privilege.DEV_POWER, description = "Empty your bank") {
            player.bank.removeAll()
            for (i in 1..9) {
                player.setVarbit("varbits.bank_tab_display" + i, 0)
            }
        }
    }
}
