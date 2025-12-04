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

class EngineLoginEvent(player: Player) : PlayerEvent(player)

fun PluginEvent.onEngineLogin(
    action: suspend EngineLoginEvent.() -> Unit
): EventListener<EngineLoginEvent> {
    return on<EngineLoginEvent> {
        then { action(this) }
    }
}