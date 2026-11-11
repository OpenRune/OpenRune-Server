package org.rsmod.api.player.input

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

public data class DialogInput(override val player: Player, public val count: Int) :
    UnboundEvent, PlayerEvent

public data class ResumePCountDialogInput(public val count: Int)
