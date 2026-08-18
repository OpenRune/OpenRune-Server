package org.rsmod.api.player.debug

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

private val COMPONENT_CLICK_DEBUG_ATTR = AttributeKey<Boolean>(temp = true)

/**
 * When enabled, [org.rsmod.api.net.rsprot.handlers.If3ButtonHandler] reports every button click
 * this player sends to the client (component RSCM name, comsub, and op)
 */
public var Player.componentClickDebug: Boolean
    get() = attr[COMPONENT_CLICK_DEBUG_ATTR] == true
    set(value) {
        attr[COMPONENT_CLICK_DEBUG_ATTR] = value
    }
