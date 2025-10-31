package org.alter.plugins.content.interfaces.gameframe.tabs.friends_list

import org.alter.api.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.social.Social

class IgnoresListPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onButton(interfaceId = Social.IGNORES_LIST_INTERFACE_ID, component = Social.SWITCH_LIST_ID) {
            player.openInterface(interfaceId = Social.FRIENDS_LIST_INTERFACE_ID, dest = InterfaceDestination.SOCIAL)
            player.setVarbit("varbits.friends_panel", 0)
        }
    }
}
