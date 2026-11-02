package org.rsmod.api.player.events.skilling

import dev.openrune.types.ItemServerType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * Published before an alchemy spell checks the item or spends runes. A subscriber can change [coins],
 * or set [cancelled] to replace the cast with its own effect: nothing is consumed and no message is
 * sent, so the subscriber owns the outcome.
 */
public class AlchemyPrepareEvent(
    override val player: Player,
    public val item: ItemServerType,
    public val slot: Int,
    public val high: Boolean,
    public var coins: Int,
    public var cancelled: Boolean = false,
) : UnboundEvent, PlayerEvent
