package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

class SendVarbitPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("sendvarbit", Privilege.DEV_POWER, description = "Send varbit to client by RSCM name or ID") {
            val values = player.getCommandArgs()
            if (values.isEmpty()) {
                player.message("Usage: ::sendvarbit <varbit_name_or_id> [value]")
                player.message("Example: ::sendvarbit varbits.prayer_rigour_unlocked 1")
                player.message("Example: ::sendvarbit 13680 1")
                return@onCommand
            }

            val varbitIdentifier = values[0]
            val value = if (values.size > 1) {
                values[1].toIntOrNull() ?: run {
                    player.message("Invalid value: ${values[1]}. Must be an integer.")
                    return@onCommand
                }
            } else {
                // Default to 1 if no value provided
                1
            }

            try {
                val varbitName = if (varbitIdentifier.toIntOrNull() != null) {
                    // It's a numeric ID, try to resolve to RSCM name
                    val numericId = varbitIdentifier.toInt()
                    RSCM.getReverseMapping(RSCMType.VARBITTYPES, numericId) ?: run {
                        player.message("Could not find varbit with ID $numericId")
                        return@onCommand
                    }
                } else {
                    // It's an RSCM name
                    varbitIdentifier
                }

                val oldValue = player.getVarbit(varbitName)
                player.setVarbit(varbitName, value)
                player.message("Sent varbit <col=0000FF>$varbitName</col> from <col=801700>$oldValue</col> to <col=801700>$value</col>")
            } catch (e: Exception) {
                player.message("Error: ${e.message}")
            }
        }
    }
}

