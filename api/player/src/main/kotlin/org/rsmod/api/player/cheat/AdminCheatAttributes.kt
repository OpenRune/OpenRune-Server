package org.rsmod.api.player.cheat

import org.rsmod.api.attr.AttributeKey
import org.rsmod.game.entity.Player

public val ADMIN_GOD_MODE_ATTR: AttributeKey<Boolean> = AttributeKey()

public val ADMIN_MAX_HIT_ATTR: AttributeKey<Boolean> = AttributeKey()

public var Player.adminGodMode: Boolean
    get() = attr[ADMIN_GOD_MODE_ATTR] == true
    set(value) {
        if (value) {
            attr[ADMIN_GOD_MODE_ATTR] = true
        } else {
            attr.remove(ADMIN_GOD_MODE_ATTR)
        }
    }

public var Player.adminMaxHit: Boolean
    get() = attr[ADMIN_MAX_HIT_ATTR] == true
    set(value) {
        if (value) {
            attr[ADMIN_MAX_HIT_ATTR] = true
        } else {
            attr.remove(ADMIN_MAX_HIT_ATTR)
        }
    }
