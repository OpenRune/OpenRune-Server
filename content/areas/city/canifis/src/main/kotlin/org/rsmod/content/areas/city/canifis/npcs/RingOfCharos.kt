package org.rsmod.content.areas.city.canifis.npcs

import org.rsmod.game.entity.Player

// https://oldschool.runescape.wiki/w/Ring_of_Charos
internal fun Player.wearsRingOfCharos(): Boolean =
    "obj.ring_of_charos" in worn || "obj.ring_of_charos_unlocked" in worn
