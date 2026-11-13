package org.rsmod.api.droptable

import dtx.core.ArgMap
import org.rsmod.game.entity.Player

/**
 * Content modules register boost callbacks here so chance rolls can adjust at roll-time without
 * dropping a dependency from this API layer onto skill content.
 */
public object DropRateBoosts {
    @Volatile
    public var clueScrollBoostPercent: (Player, ArgMap) -> Int = { _, _ -> 0 }
}
