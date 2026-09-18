package org.rsmod.content.areas.city.portsarim.travel

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.map.CoordGrid

private const val VOYAGE_JINGLE = "jingle.body_parts"
private const val FADE_CYCLES = 50

/** The boats whose crossing hides the minimap for the whole trip, like the Karamja ferry. */
internal suspend fun ProtectedAccess.sailFerry(dest: CoordGrid, destName: String, fare: Int) {
    ifClose()
    if (fare > 0) {
        mes("You pay the boarding charge of $fare coins.")
    }
    mes("You board the ship and sail to $destName.")
    midiJingle(VOYAGE_JINGLE)
    fadeOut()
    delay(1)
    minimapHideMap()
    delay(4)
    telejump(dest)
    delay(1)
    fadeIn()
    minimapReset()
    closeFadeOverlay(cycles = 2)
}

/** Veos's longer voyages to Kourend, which land the player on the ship's deck. */
internal suspend fun ProtectedAccess.sailVoyage(dest: CoordGrid, destName: String) {
    ifClose()
    midiJingle(VOYAGE_JINGLE)
    fadeOut()
    delay(3)
    mes("You board the ship and sail to $destName.")
    telejump(dest)
    delay(1)
    fadeIn()
    closeFadeOverlay(cycles = 3)
}

internal fun ProtectedAccess.payFare(fare: Int): Boolean =
    fare == 0 || (inv.count("obj.coins") >= fare && invDel(inv, "obj.coins", fare).success)

private fun ProtectedAccess.fadeOut() =
    fadeOverlay(
        startColour = 0,
        startTransparency = 255,
        endColour = 0,
        endTransparency = 0,
        clientDuration = FADE_CYCLES,
    )

private fun ProtectedAccess.fadeIn() =
    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = FADE_CYCLES,
    )
