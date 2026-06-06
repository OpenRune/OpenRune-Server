package org.rsmod.api.droptable

import org.rsmod.game.entity.Player

public fun Player.wearingRingOfWealth(): Boolean =
    "obj.ring_of_wealth" in worn || "obj.ring_of_wealth_i" in worn
