package org.rsmod.content.drops

import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.area.checker.isInWildernessBasic
import org.rsmod.game.entity.Player

private const val LOOTING_BAG = "obj.looting_bag"

public fun Player.shouldDropLootingBag(): Boolean {
    if (!coords.isInWildernessBasic()) {
        return false
    }
    return !ownsLootingBag()
}

private fun Player.ownsLootingBag(): Boolean =
    LOOTING_BAG in worn || hasObjInInventoryOrBank(LOOTING_BAG)
