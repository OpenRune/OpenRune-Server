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

class ChatanimPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {
        
    init {
        onCommand("chatanim", Privilege.DEV_POWER, description = "Chat dialogue test") {
            val args = player.getCommandArgs()
            val key = args[0].toInt()
            val npcId = args[1].toInt()

            val name = RSCM.getReverseMapping(RSCMType.SEQTYPES, key) ?: run {
                player.message("Could not find a seq animation with ID $key. Please check if the ID is valid.")
                return@onCommand
            }

            player.queue {
                chatNpc(player, "Hello World", npcId, animation = name, "hi")
            }
            player.message("$key opened in a dialog", ChatMessageType.ENGINE)
        }
    }
}
