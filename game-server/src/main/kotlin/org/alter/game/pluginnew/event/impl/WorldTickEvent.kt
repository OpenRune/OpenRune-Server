package org.alter.game.pluginnew.event.impl

import org.alter.game.model.entity.Player
import org.alter.game.pluginnew.event.Event
import org.alter.game.pluginnew.event.PlayerEvent

class WorldTickEvent(val tickCount : Long) : Event

class PlayerTickEvent(override val player: Player) : PlayerEvent(player)