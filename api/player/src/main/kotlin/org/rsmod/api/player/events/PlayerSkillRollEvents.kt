package org.rsmod.api.player.events

import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * A skilling success roll, published once per attempt. Guaranteed outcomes (a level above the
 * stop-burn level) are not rolled and publish nothing.
 *
 * `subject` is the symbol being rolled against: the npc or loc when there is one (`"npc.man"`,
 * `"loc.tree"`), otherwise the item (`"obj.raw_shrimp"`). `chance` is the odds of `success` before any
 * subscriber, from 0 to 1.
 *
 * [Result] is the final outcome after every [Modify] subscriber, so an observer does not depend on
 * subscriber order.
 */
public class PlayerSkillRollEvents {
    public data class Modify(
        override val player: Player,
        public val stat: String,
        public val subject: String?,
        public val chance: Double,
        public var success: Boolean,
    ) : UnboundEvent, PlayerEvent

    public data class Result(
        override val player: Player,
        public val stat: String,
        public val subject: String?,
        public val chance: Double,
        public val success: Boolean,
    ) : UnboundEvent, PlayerEvent
}
