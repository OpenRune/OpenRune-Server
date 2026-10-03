package org.rsmod.api.player.events

import dev.openrune.types.NpcServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player

public data class NpcExamineEvent(public val player: Player, public val type: NpcServerType) : UnboundEvent
