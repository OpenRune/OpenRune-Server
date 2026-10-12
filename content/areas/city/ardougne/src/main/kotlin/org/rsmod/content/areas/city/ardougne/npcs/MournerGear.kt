package org.rsmod.content.areas.city.ardougne.npcs

import org.rsmod.game.entity.Player

private val MOURNER_GEAR =
    listOf(
        "obj.gasmask",
        "obj.mourning_mourner_top",
        "obj.mourning_mourner_legs",
        "obj.mourning_mourner_gloves",
        "obj.mourning_mourner_boots",
        "obj.mourning_mourner_cloak",
    )

internal fun Player.wearsMournerGear(): Boolean = MOURNER_GEAR.all { worn.count(it) > 0 }

internal fun Player.ownsObj(obj: String): Boolean {
    if (obj in inv || obj in worn) {
        return true
    }
    val bank = invMap["inv.bank"]
    return bank != null && obj in bank
}
