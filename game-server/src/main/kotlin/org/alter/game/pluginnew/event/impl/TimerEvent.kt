package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Pawn
import org.alter.game.model.timer.TimerKey
import org.alter.game.pluginnew.PluginEvent
import org.alter.game.pluginnew.event.Event
import org.alter.game.pluginnew.event.EventListener

class TimerEvent(val timer: TimerKey, val player: Pawn) : Event

fun PluginEvent.onTimer(
    timerKey: TimerKey,
    action: suspend TimerEvent.() -> Unit
): EventListener<TimerEvent> {
    return on<TimerEvent> {
        where { timer == timerKey }
        then { action(this) }
    }
}