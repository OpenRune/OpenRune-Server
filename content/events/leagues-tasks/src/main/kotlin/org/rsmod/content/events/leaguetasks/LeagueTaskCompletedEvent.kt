package org.rsmod.content.events.leaguetasks

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/** Published once per task, after its bit, totals and points are written. [task] is the `action` dbrow. */
public data class LeagueTaskCompletedEvent(
    override val player: Player,
    public val league: League,
    public val task: Int,
    public val index: Int,
    public val tier: Int,
    public val points: Int,
) : UnboundEvent, PlayerEvent
