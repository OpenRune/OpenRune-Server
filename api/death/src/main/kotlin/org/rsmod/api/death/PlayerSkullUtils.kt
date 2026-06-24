package org.rsmod.api.death

import org.rsmod.api.config.constants
import org.rsmod.game.entity.Player

public fun Player.hasSkullDeathPenalty(): Boolean {
    val icon = skullIcon ?: return false
    return !constants.isForinthrySurgeSkull(icon)
}

public fun Player.isHighRiskSkulled(): Boolean {
    val icon = skullIcon ?: return false
    return icon == constants.skullicon_highrisk_world
}
