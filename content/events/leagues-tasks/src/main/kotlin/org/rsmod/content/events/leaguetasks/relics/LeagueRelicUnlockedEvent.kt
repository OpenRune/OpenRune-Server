package org.rsmod.content.events.leaguetasks.relics

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/** Published once the unlock message is shown, a few ticks after the choice was confirmed. */
public data class LeagueRelicUnlockedEvent(
    override val player: Player,
    public val tier: LeagueRelics.RelicTier,
    public val relic: LeagueRelics.Relic,
) : UnboundEvent, PlayerEvent
