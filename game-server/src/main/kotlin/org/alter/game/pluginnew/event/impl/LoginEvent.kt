package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Player
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.EventListener
import org.alter.game.pluginnew.event.PlayerEvent

class LoginEvent(player: Player) : PlayerEvent(player)

fun PluginEvent.onLogin(
    action: suspend LoginEvent.() -> Unit
): EventListener<LoginEvent> {
    return on<LoginEvent> {
        then { action(this) }
    }
}

class PlayerDeathEvent(player: Player) : PlayerEvent(player)

fun PluginEvent.onPlayerDeath(
    action: suspend PlayerDeathEvent.() -> Unit
): EventListener<PlayerDeathEvent> {
    return on<PlayerDeathEvent> {
        then { action(this) }
    }
}

class LogoutEvent(player: Player) : PlayerEvent(player)

fun PluginEvent.onLogout(
    action: suspend LogoutEvent.() -> Unit
): EventListener<LogoutEvent> {
    return on<LogoutEvent> {
        then { action(this) }
    }
}


class EngineLoginEvent(player: Player) : PlayerEvent(player)

fun PluginEvent.onEngineLogin(
    action: suspend EngineLoginEvent.() -> Unit
): EventListener<EngineLoginEvent> {
    return on<EngineLoginEvent> {
        then { action(this) }
    }
}