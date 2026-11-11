package org.rsmod.content.events.leaguetasks.pacts

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/** Published once the unlock emote has played, five ticks after [nodes] were unlocked. */
public class LeaguePactsSpentEvent(override val player: Player, public val nodes: IntArray) :
    UnboundEvent, PlayerEvent
