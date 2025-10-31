package org.alter.plugins.content.commands.commands.developer

import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.dsl.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.model.attr.*
import org.alter.game.model.container.*
import org.alter.game.model.container.key.*
import org.alter.game.model.entity.*
import org.alter.game.model.item.*
import org.alter.game.model.priv.Privilege
import org.alter.game.model.queue.*
import org.alter.game.model.shop.*
import org.alter.game.model.timer.*
import org.alter.game.plugin.*
import org.alter.rscm.RSCM
import org.alter.rscm.RSCMType

class ObjPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("obj", Privilege.DEV_POWER, description = "Spawn object by id") {
            val values = player.getCommandArgs()
            val id = values[0].toInt()

            val name = RSCM.getReverseMapping(RSCMType.LOCTYPES, id) ?: run {
                player.message("Could not find a object with ID $id. Please check if the ID is valid.")
                return@onCommand
            }


            val type = if (values.size > 1) values[1].toInt() else 10
            val rot = if (values.size > 2) values[2].toInt() else 0
            val obj = DynamicObject(name, type, rot, player.tile)
            player.message("Adding object to: ")
            world.spawn(obj)
        }
    }
}
