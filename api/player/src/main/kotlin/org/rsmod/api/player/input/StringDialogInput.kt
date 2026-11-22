package org.rsmod.api.player.input

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public data class StringDialogInput(
    public val player: Player,
    public val text: String,
) : UnboundEvent
