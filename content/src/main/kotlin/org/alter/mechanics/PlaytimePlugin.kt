package org.alter.mechanics

import org.alter.api.ext.player
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.PLAYTIME_ATTR
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.impl.LoginEvent
import org.alter.game.pluginnew.event.impl.TimerEvent
import org.alter.game.pluginnew.event.impl.onLogin
import org.alter.game.pluginnew.event.impl.onTimer

/**
 * Plugin that tracks player playtime by incrementing a counter every game cycle.
 *
 * @author Auto-generated
 */
class PlaytimePlugin: PluginEvent() {

    companion object {
        /**
         * Timer key for playtime tracking. This timer fires every cycle to increment playtime.
         */
        val PLAYTIME_TIMER = TimerKey("playtime_timer", tickOffline = false)
    }

    override fun init() {
        onLogin {
            if (player.attr[PLAYTIME_ATTR] == null) {
                player.attr[PLAYTIME_ATTR] = 0
            }
            player.timers[PLAYTIME_TIMER] = 1
        }

        onTimer(PLAYTIME_TIMER) {
            val currentPlaytime = player.attr[PLAYTIME_ATTR] ?: 0
            player.attr[PLAYTIME_ATTR] = currentPlaytime + 1
            player.timers[PLAYTIME_TIMER] = 1
        }
    }
}