package org.alter.mechanics.run

import org.alter.api.ext.getVarp
import org.alter.api.ext.setVarp
import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton
import org.alter.game.pluginnew.event.impl.onLogin
import org.alter.game.pluginnew.event.impl.onTimer

class RunEnergyPlugin() : PluginEvent() {

    override fun init() {
        onLogin {
            player.timers[RunEnergy.RUN_DRAIN] = 1
            // Enable run by default if not already set (persists across logins)
            // If the varp is 0 (not set or disabled), enable it (set to 1)
            if (player.getVarp(RunEnergy.RUN_ENABLED_VARP) == 0) {
                player.setVarp(RunEnergy.RUN_ENABLED_VARP, 1)
            }
        }

        onTimer(RunEnergy.RUN_DRAIN) {
            player.timers[RunEnergy.RUN_DRAIN] = 1
            RunEnergy.drain(player as Player)
        }

        onButton("components.orbs:runbutton") {
            RunEnergy.toggle(player)
        }

        onButton("components.settings_side:runmode") {
            RunEnergy.toggle(player)
        }
    }
}
