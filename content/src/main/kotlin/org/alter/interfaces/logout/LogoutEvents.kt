package org.alter.interfaces.logout

import net.rsprot.protocol.game.outgoing.logout.Logout
import org.alter.api.ext.message
import org.alter.game.model.timer.ACTIVE_COMBAT_TIMER
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.onButton

class LogoutEvents : PluginEvent() {

    override fun init() {

        onButton("components.logout:logout") {
            if (!player.timers.has(ACTIVE_COMBAT_TIMER)) {
                player.requestLogout()
                player.write(Logout)
                player.session?.requestClose()
                player.channelFlush()
            } else {
                player.message("You can't log out until 10 seconds after the end of combat.")
            }
        }


    }

}