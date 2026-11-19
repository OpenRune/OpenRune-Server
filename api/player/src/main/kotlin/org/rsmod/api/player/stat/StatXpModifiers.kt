package org.rsmod.api.player.stat

import org.rsmod.game.entity.Player

/**
 * Server-wide hooks on every naturally earned xp drop, on top of the player's own xp rate. Game
 * modes such as leagues install these at startup.
 * - [modifier] multiplies the drop.
 * - [bonus] adds flat xp after the multiplier.
 * - [onGained] is told how much xp was actually added.
 */
public object StatXpModifiers {
    @Volatile public var modifier: (Player, String) -> Double = { _, _ -> 1.0 }
    @Volatile public var bonus: (Player, String) -> Double = { _, _ -> 0.0 }
    @Volatile public var onGained: (Player, String, Int) -> Unit = { _, _, _ -> }
}
