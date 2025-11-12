package org.alter.plugins.content.interfaces.gameframe.tabs.emotes

import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.emotes.Emote
import org.alter.plugins.content.interfaces.gameframe.tabs.emotes.EmotesTab.COMPONENT_ID
import org.alter.plugins.content.interfaces.gameframe.tabs.emotes.EmotesTab.performEmote

class EmotesTabPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onLogin {
            player.setInterfaceEvents(
                interfaceId = COMPONENT_ID,
                component = 2,
                range = 0..51,
                setting = arrayOf(InterfaceEvent.ClickOp1, InterfaceEvent.ClickOp2),
            )
        }

        onButton(interfaceId = COMPONENT_ID, component = 2) p@{
            val slot = player.getInteractingSlot()
            val emote = Emote.values.firstOrNull { e -> e.slot == slot } ?: return@p
            performEmote(player, emote)
        }
    }
}
