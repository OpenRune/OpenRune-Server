package org.rsmod.content.generic.npcs.ghost

import org.rsmod.game.entity.Player

// https://oldschool.runescape.wiki/w/Ghostspeak_amulet
internal fun Player.understandsGhosts(): Boolean = GHOSTSPEAK.any { it in worn }

private val GHOSTSPEAK =
    listOf(
        "obj.amulet_of_ghostspeak",
        "obj.amulet_of_ghostspeak_enchanted",
        "obj.morytania_legs_medium",
        "obj.morytania_legs_hard",
        "obj.morytania_legs_elite",
    )
