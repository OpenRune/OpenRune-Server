package org.alter.game.model.interf.listener

import org.alter.game.model.entity.Player
import org.alter.game.plugin.PluginRepository
import org.alter.game.pluginnew.event.impl.InterfaceCloseEvent
import org.alter.game.pluginnew.event.impl.InterfaceOpenEvent

/**
 * @author Tom <rspsmods@gmail.com>
 */
class PlayerInterfaceListener(val player: Player, val plugins: PluginRepository) : InterfaceListener {
    override fun onInterfaceOpen(interfaceId: Int) {
        plugins.executeInterfaceOpen(player, interfaceId)
        InterfaceOpenEvent(player,interfaceId).post()
    }

    override fun onInterfaceClose(interfaceId: Int) {
        player.world.plugins.executeInterfaceClose(player, interfaceId)
        InterfaceCloseEvent(player, interfaceId).post()
    }
}
