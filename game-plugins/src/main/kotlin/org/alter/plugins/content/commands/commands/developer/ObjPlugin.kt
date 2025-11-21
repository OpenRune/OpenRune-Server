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
            if (values.isEmpty()) {
                player.message("Usage: ::obj <id> [type] [rotation]")
                player.message("Example: ::obj 1535 0 0")
                return@onCommand
            }

            val id = try {
                values[0].toInt()
            } catch (e: NumberFormatException) {
                player.message("Invalid ID: ${values[0]}. Must be a number.")
                return@onCommand
            }

            val name = RSCM.getReverseMapping(RSCMType.LOCTYPES, id) ?: run {
                player.message("Could not find an object with ID $id. Please check if the ID is valid.")
                return@onCommand
            }

            // Default type: 0 for doors/walls, 10 for regular objects
            // Check if the name suggests it's a door/gate/fence
            val isDoorLike = name.contains("door", ignoreCase = true) ||
                           name.contains("gate", ignoreCase = true) ||
                           name.contains("fence", ignoreCase = true) ||
                           name.contains("wall", ignoreCase = true)
            val defaultType = if (isDoorLike) 0 else 10

            val type = if (values.size > 1) {
                try {
                    values[1].toInt()
                } catch (e: NumberFormatException) {
                    player.message("Invalid type: ${values[1]}. Must be a number.")
                    return@onCommand
                }
            } else {
                defaultType
            }

            val rot = if (values.size > 2) {
                try {
                    values[2].toInt()
                } catch (e: NumberFormatException) {
                    player.message("Invalid rotation: ${values[2]}. Must be a number.")
                    return@onCommand
                }
            } else {
                0
            }

            val obj = DynamicObject(name, type, rot, player.tile)
            player.message("Spawning object: $name (ID: $id, Type: $type, Rotation: $rot) at ${player.tile}")
            world.spawn(obj)

            // Note: Collision is automatically added by world.spawn() via Chunk.addEntity()
        }

        onCommand("object", Privilege.DEV_POWER, description = "Spawn object by id") {
            val values = player.getCommandArgs()
            if (values.isEmpty()) {
                player.message("Usage: ::object <id> [type] [rotation]")
                player.message("Example: ::object 1535 0 0")
                return@onCommand
            }

            val id = try {
                values[0].toInt()
            } catch (e: NumberFormatException) {
                player.message("Invalid ID: ${values[0]}. Must be a number.")
                return@onCommand
            }

            val name = RSCM.getReverseMapping(RSCMType.LOCTYPES, id) ?: run {
                player.message("Could not find an object with ID $id. Please check if the ID is valid.")
                return@onCommand
            }

            // Default type: 0 for doors/walls, 10 for regular objects
            // Check if the name suggests it's a door/gate/fence
            val isDoorLike = name.contains("door", ignoreCase = true) ||
                           name.contains("gate", ignoreCase = true) ||
                           name.contains("fence", ignoreCase = true) ||
                           name.contains("wall", ignoreCase = true)
            val defaultType = if (isDoorLike) 0 else 10

            val type = if (values.size > 1) {
                try {
                    values[1].toInt()
                } catch (e: NumberFormatException) {
                    player.message("Invalid type: ${values[1]}. Must be a number.")
                    return@onCommand
                }
            } else {
                defaultType
            }

            val rot = if (values.size > 2) {
                try {
                    values[2].toInt()
                } catch (e: NumberFormatException) {
                    player.message("Invalid rotation: ${values[2]}. Must be a number.")
                    return@onCommand
                }
            } else {
                0
            }

            val obj = DynamicObject(name, type, rot, player.tile)
            player.message("Spawning object: $name (ID: $id, Type: $type, Rotation: $rot) at ${player.tile}")
            world.spawn(obj)

            // Note: Collision is automatically added by world.spawn() via Chunk.addEntity()
        }
    }
}
