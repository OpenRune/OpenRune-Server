package org.rsmod.api.net.rsprot.player

import net.rsprot.protocol.api.Session
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

data class SessionStart(override val player: Player, val session: Session<Player>) :
    UnboundEvent, PlayerEvent
