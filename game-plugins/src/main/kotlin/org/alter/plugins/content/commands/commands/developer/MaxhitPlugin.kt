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
import org.alter.api.Colors
import org.alter.plugins.content.combat.Combat

class MaxhitPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("maxhit", Privilege.DEV_POWER, description = "Toggle always hitting max damage") {
            val enabled = player.attr[Combat.ALWAYS_MAX_HIT] ?: false
            player.attr[Combat.ALWAYS_MAX_HIT] = !enabled
            val status = if (!enabled) "<col=${Colors.GREEN}>enabled</col>" else "<col=${Colors.RED}>disabled</col>"
            player.message("Always max hit: $status")
        }
    }
}

