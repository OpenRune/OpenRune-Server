package org.rsmod.api.player.events

import dev.openrune.types.StatType
import org.rsmod.events.UnboundEvent
import org.rsmod.game.entity.Player
import org.rsmod.game.entity.player.PlayerEvent

/**
 * Observers of the level engine queues, which allow one script per key: `LevelUpScript` holds the
 * default `onAdvanceStat`, and a labelled script for a stat would silently replace it. Any number of
 * scripts can subscribe to these, and they fire before the engine queue script.
 */
public class PlayerLevelEvents {
    /** A base level rose. [level] is the new level; a jump of several levels is one event. */
    public data class Advanced(
        override val player: Player,
        public val stat: StatType,
        public val previousLevel: Int,
        public val level: Int,
    ) : UnboundEvent, PlayerEvent

    public data class CombatAdvanced(override val player: Player, public val level: Int) :
        UnboundEvent, PlayerEvent

    /** A current or base level changed for any reason, including boosts, drains and damage. */
    public data class StatChanged(override val player: Player, public val stat: StatType) :
        UnboundEvent, PlayerEvent
}

/**
 * Observers of `onArea` and `onAreaExit`, which allow one script per area. Exits also fire on
 * logout.
 */
public class PlayerAreaEvents {
    public data class Entered(override val player: Player, public val area: Int) :
        UnboundEvent, PlayerEvent

    public data class Exited(override val player: Player, public val area: Int) :
        UnboundEvent, PlayerEvent
}
