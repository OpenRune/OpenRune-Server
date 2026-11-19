package org.rsmod.api.player.stat

import org.rsmod.game.entity.Player

/**
 * A server-wide, per-stat xp multiplier applied to every naturally earned xp drop, on top of the
 * player's own xp rate. Game modes such as leagues install [modifier] at startup.
 */
public object StatXpModifiers {
    @Volatile public var modifier: (Player, String) -> Double = { _, _ -> 1.0 }
}
