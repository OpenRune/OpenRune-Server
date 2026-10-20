package org.rsmod.content.quest.util

import org.rsmod.api.player.protect.ProtectedAccess

internal suspend fun ProtectedAccess.fadeToBlack() {
    fadeOverlay(
        startColour = 0,
        startTransparency = 255,
        endColour = 0,
        endTransparency = 0,
        clientDuration = FADE_CLIENT_DURATION,
    )
    delay(FADE_CYCLES)
}

internal suspend fun ProtectedAccess.fadeFromBlack() {
    fadeOverlay(
        startColour = 0,
        startTransparency = 0,
        endColour = 0,
        endTransparency = 255,
        clientDuration = FADE_CLIENT_DURATION,
    )
    delay(FADE_CYCLES)
    closeFadeOverlay()
}

private const val FADE_CLIENT_DURATION = 50
private const val FADE_CYCLES = 3
