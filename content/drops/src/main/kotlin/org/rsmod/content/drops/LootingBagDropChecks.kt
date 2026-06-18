package org.rsmod.content.drops

import org.rsmod.api.area.checker.isInWilderness
import org.rsmod.api.area.checker.isInWildernessBasic
import org.rsmod.game.entity.Player

private const val LOOTING_BAG = "obj.looting_bag"
private const val INV = "inv.inv"
private const val BANK = "inv.bank"

public fun Player.shouldDropLootingBag(): Boolean {
    if (!coords.isInWildernessBasic()) {
        return false
    }
    return !ownsLootingBag()
}

private fun Player.ownsLootingBag(): Boolean {
    if (LOOTING_BAG in worn) {
        return true
    }
    val inv = invMap[INV]
    if (inv != null && LOOTING_BAG in inv) {
        return true
    }
    val bank = invMap[BANK]
    return bank != null && LOOTING_BAG in bank
}
