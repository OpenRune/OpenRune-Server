package org.rsmod.api.player.events

import dev.openrune.types.SequenceServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * Published when an emote starts playing.
 *
 * @param emote the name shown in the emote tab (`Bow`, `Skill Cape`, ...), the same for an emote's
 *   looping and non-looping variants.
 * @param seq the sequence that was played, which tells variants and skill capes apart.
 */
public data class PlayerEmoteEvent(
    override val player: Player,
    public val emote: String,
    public val seq: SequenceServerType,
) : UnboundEvent, PlayerEvent
