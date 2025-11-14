package org.alter.plugins.content.commands.commands.developer

import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Minimal command registration to prevent "No valid command found" message.
 * Actual command logic is handled in content/src/main/kotlin/org/alter/commands/developer/UnlockPrayersPlugin.kt
 * via CommandEvent.
 */
class UnlockPrayersPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("unlockprayers", Privilege.DEV_POWER, description = "Toggle all prayer varbits on/off") {
            // Command is handled by the new PluginEvent system via CommandEvent
            // This registration just prevents the "No valid command found" message
        }
    }
}

