package org.rsmod.api.player.ironman

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

public val SAFE_DEATH_ATTR: AttributeKey<Boolean> = AttributeKey()

public fun Player.markNextDeathSafe() {
    attr[SAFE_DEATH_ATTR] = true
}

public fun Player.clearSafeDeathMark() {
    attr.remove(SAFE_DEATH_ATTR)
}

public fun Player.hasSafeDeathMark(): Boolean = attr[SAFE_DEATH_ATTR] == true
