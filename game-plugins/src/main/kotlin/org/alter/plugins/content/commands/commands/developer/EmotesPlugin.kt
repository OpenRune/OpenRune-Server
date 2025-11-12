package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.gameframe.tabs.emotes.EmotesTab

class EmotesPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("emotes", Privilege.DEV_POWER, description = "Unlock all emotes") {
            EmotesTab.unlockAll(player)
            player.message("All emotes were unlocked.")
        }
    }
}
