package org.alter.plugins.content.mechanics.playtime

import org.alter.api.ext.*
import org.alter.game.Server
import org.alter.game.model.World
import org.alter.game.model.attr.PLAYTIME_ATTR
import org.alter.game.model.timer.TimerKey
import org.alter.game.plugin.KotlinPlugin
import org.alter.game.plugin.PluginRepository

/**
 * Plugin that tracks player playtime by incrementing a counter every game cycle.
 *
 * @author Auto-generated
 */
class PlaytimePlugin(
    r: PluginRepository,
    world: World,
    server: Server
) : KotlinPlugin(r, world, server) {

    companion object {
        /**
         * Timer key for playtime tracking. This timer fires every cycle to increment playtime.
         */
        val PLAYTIME_TIMER = TimerKey("playtime_timer", tickOffline = false)
    }

    init {
        onLogin {
            // Initialize playtime if not set
            if (player.attr[PLAYTIME_ATTR] == null) {
                player.attr[PLAYTIME_ATTR] = 0
            }
            // Start the playtime timer
            player.timers[PLAYTIME_TIMER] = 1
        }

        onTimer(PLAYTIME_TIMER) {
            // Increment playtime by 1 cycle
            val currentPlaytime = player.attr[PLAYTIME_ATTR] ?: 0
            player.attr[PLAYTIME_ATTR] = currentPlaytime + 1
            // Reset timer to fire again next cycle
            player.timers[PLAYTIME_TIMER] = 1
        }
    }
}

