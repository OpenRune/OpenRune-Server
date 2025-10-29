package org.alter.plugins.content.interfaces.gameframe.orbs.xpdrops

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.gameframe.config.Orbs
import org.alter.plugins.content.interfaces.xpdrops.XpSettings

class XpDropsPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {

        onButton(interfaceId = Orbs.ORBS_UNIVERSE, component = Orbs.XP_ORB) {
            val option = player.getInteractingOption()
            player.playSound(Sound.INTERFACE_SELECT1)

            when (option) {
                1 -> {
                    player.toggleVarbit(Orbs.XPDROPSENABLED)
                    if (player.getVarbit(Orbs.XPDROPSENABLED) == 1) {
                        player.openInterface(Orbs.XP_DROPS, InterfaceDestination.XP_COUNTER)
                    } else {
                        player.closeInterface(Orbs.XP_DROPS)
                    }
                }
                2 -> {
                    if (player.lock.canInterfaceInteract()) {
                        XpSettings.open(player)
                    }
                }
            }
        }
    }
}
