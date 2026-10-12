package org.rsmod.content.areas.city.portphasmatys.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.game.entity.Player

// https://oldschool.runescape.wiki/w/Ghostspeak_amulet
internal fun Player.understandsGhosts(): Boolean = GHOSTSPEAK.any { it in worn }

internal suspend fun Dialogue.wail(
    text: String = "Woooo wooo wooooo woooo",
    cannotUnderstand: Boolean = true,
) {
    chatNpc(neutral, text)
    if (cannotUnderstand) {
        mesbox("You cannot understand the ghost.")
    }
}

private val GHOSTSPEAK =
    listOf(
        "obj.amulet_of_ghostspeak",
        "obj.amulet_of_ghostspeak_enchanted",
        "obj.morytania_legs_medium",
        "obj.morytania_legs_hard",
        "obj.morytania_legs_elite",
    )
