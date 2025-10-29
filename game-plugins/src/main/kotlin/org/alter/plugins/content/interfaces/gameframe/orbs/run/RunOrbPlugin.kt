package org.alter.plugins.content.interfaces.gameframe.orbs.run


import org.alter.api.*
import org.alter.api.cfg.*
import org.alter.api.ext.*
import org.alter.game.*
import org.alter.game.model.*
import org.alter.game.plugin.*
import org.alter.plugins.content.interfaces.gameframe.config.Orbs
import org.alter.plugins.content.interfaces.xpdrops.XpSettings

class RunOrbPlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    init {

        onButton(interfaceId = Orbs.ORBS_UNIVERSE, component = Orbs.RUN_ORB) {
            val option = player.getInteractingOption()
            player.playSound(Sound.INTERFACE_SELECT1)
            player.toggleVarp(Orbs.RUNVARP)
        }
    }
}
