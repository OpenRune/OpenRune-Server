package org.rsmod.content.areas.city.varlamore.npcs

import org.rsmod.api.player.dialogue.Dialogue
import org.rsmod.api.player.stat.stat
import org.rsmod.api.player.stat.statBase
import org.rsmod.api.player.stat.statHeal

internal fun Dialogue.isWounded(): Boolean =
    player.stat(HITPOINTS) < player.statBase(HITPOINTS)

// https://oldschool.runescape.wiki/w/Nelta_of_Ralos: heals 2 + floor(Hitpoints / 5).
internal fun Dialogue.ralosHeal() {
    player.statHeal(HITPOINTS, constant = 2, percent = 20)
}

private const val HITPOINTS = "stat.hitpoints"
