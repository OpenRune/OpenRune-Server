package org.alter.plugins.content.commands.commands.developer

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

class VarpPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("varp", Privilege.DEV_POWER, description = "Set varp to amount") {
            val args = player.getCommandArgs()
            val varp = args[0].toInt()
            val state = args[1].toInt()

            val name = RSCM.getReverseMapping(RSCMType.VARPTYPES, varp) ?: run {
                player.message("Could not find a varp with ID $varp. Please check if the ID is valid.")
                return@onCommand
            }

            val oldState = player.getVarp(name)
            player.setVarp(name, state)
            player.message("Set varp (<col=801700>$varp</col>) from <col=801700>$oldState</col> to <col=801700>${player.getVarp(name)}</col>")
        }

        onCommand("getvarp") {
            val args = player.getCommandArgs()

            val varp = args[0].toInt()

            val name = RSCM.getReverseMapping(RSCMType.VARPTYPES, varp) ?: run {
                player.message("Could not find a varp with ID $varp. Please check if the ID is valid.")
                return@onCommand
            }

            val varpState = player.getVarp(name)
            player.message("${args[0]} Varp state: $varpState")
        }
    }
}
