package org.rsmod.api.player.input

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public data class DialogInput(val player: Player, public val count : Int) : UnboundEvent

public data class ResumePCountDialogInput(public val count: Int)
