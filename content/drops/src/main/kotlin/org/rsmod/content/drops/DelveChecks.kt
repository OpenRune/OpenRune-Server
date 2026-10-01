package org.rsmod.content.drops

import org.rsmod.api.player.vars.intVarp
import org.rsmod.game.entity.Player

private var Player.currentDelveVarp by intVarp("varp.dom_current_level_temp")

public fun Player.hasReachedDelve(level: Int): Boolean = currentDelveVarp + 1 >= level
