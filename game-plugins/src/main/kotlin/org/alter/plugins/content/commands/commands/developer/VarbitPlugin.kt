package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

class VarbitPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("varbit", Privilege.DEV_POWER, description = "Get or set varbit value") {
            val args = player.getCommandArgs()

            if (args.isEmpty()) {
                player.message("Usage: ::varbit <id> [value]")
                return@onCommand
            }

            val varbitId = args[0].toIntOrNull()
            if (varbitId == null) {
                player.message("Invalid varbit id. Must be a number.")
                return@onCommand
            }

            // Alleen uitlezen
            if (args.size == 1) {
                val currentValue = player.getVarbit(varbitId)
                player.message("Varbit (<col=801700>$varbitId</col>) = <col=801700>$currentValue</col>")
                return@onCommand
            }

            // Uitlezen én instellen
            val newValue = args[1].toIntOrNull()
            if (newValue == null) {
                player.message("Invalid value. Must be a number.")
                return@onCommand
            }

            val oldValue = player.getVarbit(varbitId)
            player.setVarbit(varbitId, newValue)
            val updatedValue = player.getVarbit(varbitId)

            player.message(
                "Set varbit (<col=801700>$varbitId</col>) from " +
                        "<col=801700>$oldValue</col> to <col=801700>$updatedValue</col>"
            )
        }
    }
}
