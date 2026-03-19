package org.rsmod.content.interfaces.emotes

import dev.openrune.types.SequenceServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

data class PlayEmote(val player: Player, val seq: SequenceServerType) : UnboundEvent
