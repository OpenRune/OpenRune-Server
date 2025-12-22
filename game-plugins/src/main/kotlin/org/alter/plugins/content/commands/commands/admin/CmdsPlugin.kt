package org.alter.plugins.content.commands.commands.admin

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

class CmdsPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        onCommand(command = "cs", powerRequired = Privilege.OWNER_POWER, description = "Run client script by ID with optional arguments") {
            val args = player.getCommandArgs()
            if (args.isEmpty()) {
                player.message("Usage: ::cs <script_id> [args...]")
            } else {
                try {
                    val scriptId = args[0].toInt()
                    // Convert remaining arguments to appropriate types (int if numeric, string otherwise)
                    val clientArgs = mutableListOf<Any>()
                    for (i in 1 until args.size) {
                        val arg = args[i]
                        clientArgs.add(arg.toIntOrNull() ?: arg)
                    }
                    player.runClientScript(ClientScript(id = scriptId), *clientArgs.toTypedArray())
                    player.message("Executed client script: $scriptId${if (clientArgs.isNotEmpty()) " with args: ${clientArgs.joinToString()}" else ""}")
                } catch (e: NumberFormatException) {
                    player.message("Invalid script ID. Must be an integer.")
                }
            }
        }

        fun getCommands(r: PluginRepository): List<String> {
            val str_list = ArrayList<String>()
            r.commandPlugins.forEach { (t, _) ->
                var value = "::$t"
                if (r.getDescription(t) != "") {
                    value += " = [ ${r.getDescription(t)} ]"
                }
                str_list.add(value)
            }
            return str_list
        }
    }
}
