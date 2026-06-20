package org.rsmod.api.death

import org.rsmod.api.player.death.DeathCause
import org.rsmod.api.player.death.recordDeathCause
import org.rsmod.game.entity.Player

public fun Player.preparePvpDeath(killer: Player) {
    attr[LAST_PVP_HIT_TICK_ATTR] = currentMapClock
    recordDeathCause(DeathCause.ByPlayer(killer))
}

public fun Player.prepareAdminDieTest() {
    attr[DEATH_DROPS_BYPASS_ADMIN_ATTR] = true
}

internal fun Player.deathDropsBypassAdmin(): Boolean =
    attr.getOrDefault(DEATH_DROPS_BYPASS_ADMIN_ATTR, false)
