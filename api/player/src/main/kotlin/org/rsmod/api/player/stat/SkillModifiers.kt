package org.rsmod.api.player.stat

import org.rsmod.game.entity.Player

/** Server-wide skilling hooks that game modes such as leagues install at startup. */
public object SkillModifiers {
    /** Adjusts the success chance of a stat roll; returns the new chance in `0.0..1.0`. */
    @Volatile public var successRate: (Player, String, Double) -> Double = { _, _, rate -> rate }

    /** Multiplies the loot of a skilling action such as thieving. */
    @Volatile public var lootMultiplier: (Player, String) -> Int = { _, _ -> 1 }

    /** When `true`, eating food does not use it up. */
    @Volatile public var keepsFood: (Player) -> Boolean = { false }

    public fun alwaysSucceeds(player: Player, stat: String): Boolean =
        successRate(player, stat, 0.0) >= 1.0
}
