package org.alter.plugins.content.commands.commands.admin

import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.inv.invtx.invAdd
import org.alter.game.model.priv.Privilege
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

class InvTest(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {
        onCommand("invtest", Privilege.ADMIN_POWER) {
            player.invAdd(player.inventory,"items.coins",400)
            player.invAdd(player.inventory,"items.shark",2)
            player.invAdd(player.inventory,"items.cert_shark",10000)
            player.invAdd(player.inventory,"items.mcannonremains",10)


        }

    }
}
